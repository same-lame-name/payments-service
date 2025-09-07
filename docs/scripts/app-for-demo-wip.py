from flask import Flask, request, render_template_string, jsonify
import requests
import json
from threading import Lock, Thread

app = Flask(__name__)

# Use a list as a queue to store all incoming webhooks
webhook_responses = []
lock = Lock() # To make list operations thread-safe

# HTML and JavaScript template with final logic
HTML_TEMPLATE = """
<!DOCTYPE html>
<html>
<head>
    <title>Dynamic Webhook Tester</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f4f4f9; }
        #formContainer { padding: 20px; background: white; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); transition: opacity 0.5s; max-width: 800px; margin: auto; }
        #formContainer.hidden { opacity: 0; pointer-events: none; }
        textarea, input, select { width: 100%; margin-bottom: 10px; padding: 8px; box-sizing: border-box; border: 1px solid #ccc; border-radius: 4px; }
        textarea { height: 200px; font-family: monospace; }
        button { background-color: #007bff; color: white; padding: 10px 15px; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }
        button:hover { background-color: #0056b3; }
        
        .checkbox-container { display: flex; align-items: center; margin-bottom: 10px; }
        #realtimeCheckbox { width: auto; margin-right: 8px; }

        .loader {
            display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(8px); -webkit-backdrop-filter: blur(8px);
            flex-direction: column; align-items: center; justify-content: center; z-index: 1000;
        }
        .loader.active { display: flex; }
        
        #loaderText { color: #0056b3; font-size: 20px; margin-top: 25px; font-weight: bold; }
        
        @keyframes bounce { 0%, 100% { transform: scaleY(0.4); } 50% { transform: scaleY(1); } }
        .fancy-loader { display: flex; justify-content: space-between; width: 80px; height: 50px; }
        .fancy-loader div { width: 12px; background-color: #007bff; animation: bounce 1.2s infinite ease-in-out; border-radius: 4px; }
        .fancy-loader div:nth-child(2) { animation-delay: -1.1s; }
        .fancy-loader div:nth-child(3) { animation-delay: -1.0s; }
        .fancy-loader div:nth-child(4) { animation-delay: -0.9s; }

        .error { color: red; padding: 10px; background-color: #ffebee; border: 1px solid #e57373; border-radius: 4px; margin-top: 10px;}
        
        #responseContainer {
            display: none; position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            text-align: center; padding: 50px; box-sizing: border-box; font-size: 24px;
            z-index: 999; flex-direction: column; align-items: center; justify-content: center;
            transition: background-color 0.5s;
        }
        #responseContainer.active { display: flex; }
        #responseContainer.success { background-color: #28a745; color: white; }
        #responseContainer.failed { background-color: #dc3545; color: white; }
        #responseContainer.other { background-color: #ffc107; color: #212529; }
        
        #responseContent { 
            background-color: rgba(255, 255, 255, 0.9); width: 80%; max-width: 800px; padding: 20px; 
            border-radius: 8px; text-align: left; white-space: pre-wrap; word-wrap: break-word; 
            font-family: monospace; font-size: 16px; max-height: 70vh; overflow-y: auto; color: #333; 
        }
        #tryAgain { font-size: 24px; padding: 10px 20px; margin-top: 20px; cursor: pointer; }
    </style>
</head>
<body>
    <div id="formContainer">
        <h2>Post Payment Request</h2>
        <form id="paymentForm">
            <label for="backendUrl">Target URL:</label>
            <input type="text" id="backendUrl" value="http://localhost:8080/api/v2/book-transfers/payment"><br>
            <label for="httpMethod">HTTP Method:</label>
            <select id="httpMethod">
                <option value="POST" selected>POST</option>
                <option value="PUT">PUT</option>
            </select><br>
            
            <div class="checkbox-container">
                <input type="checkbox" id="realtimeCheckbox">
                <label for="realtimeCheckbox">Real time alerts</label>
            </div>

            <label for="jsonPayload">JSON Payload:</label>
            <textarea id="jsonPayload">{{ default_payload }}</textarea><br>
            <button type="submit">Post</button>
        </form>
        <div id="error" class="error" style="display:none;"></div>
    </div>

    <div id="loader" class="loader">
        <div class="fancy-loader">
            <div></div> <div></div> <div></div> <div></div>
        </div>
        <p id="loaderText"></p>
    </div>
    
    <div id="responseContainer">
        <h3 id="responseTitle"></h3>
        <pre id="responseContent"></pre>
        <button id="tryAgain">Try again</button>
    </div>

    <script>
        let pollingInterval = null;
        let isProcessingQueue = false;

        const errorDiv = document.getElementById('error');
        const formContainer = document.getElementById('formContainer');
        const loader = document.getElementById('loader');
        const responseContainer = document.getElementById('responseContainer');
        const responseTitle = document.getElementById('responseTitle');
        const realtimeCheckbox = document.getElementById('realtimeCheckbox');
        const jsonPayloadTextarea = document.getElementById('jsonPayload');
        const loaderText = document.getElementById('loaderText');

        realtimeCheckbox.addEventListener('change', () => {
            try {
                let payload = JSON.parse(jsonPayloadTextarea.value);
                if (realtimeCheckbox.checked) {
                    payload.realtime = true;
                } else {
                    delete payload.realtime;
                }
                jsonPayloadTextarea.value = JSON.stringify(payload, null, 4);
            } catch (e) {
                console.error("Invalid JSON in textarea, cannot modify.", e);
            }
        });
        
        document.getElementById('paymentForm').addEventListener('submit', async (e) => {
            e.preventDefault();
            
            errorDiv.textContent = '';
            errorDiv.style.display = 'none';
            formContainer.classList.add('hidden');
            
            if (realtimeCheckbox.checked) {
                loaderText.textContent = "Getting real time status for you...";
            } else {
                loaderText.textContent = "Your transaction is getting processed...";
            }
            loader.classList.add('active');

            try {
                pollingInterval = setInterval(checkWebhook, 1000);

                const payload = jsonPayloadTextarea.value;
                const url = document.getElementById('backendUrl').value;
                const method = document.getElementById('httpMethod').value;
                JSON.parse(payload);
                const response = await fetch('/submit', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ payload, url, method })
                });

                if (!response.ok) {
                    const result = await response.json();
                    throw new Error(result.error || `Request to /submit failed: ${response.status}`);
                }
                
            } catch (error) {
                if (pollingInterval) clearInterval(pollingInterval);
                errorDiv.textContent = 'Error: ' + error.message;
                errorDiv.style.display = 'block';
                loader.classList.remove('active');
                formContainer.classList.remove('hidden');
            }
        });

        function processResponseQueue(queue) {
            isProcessingQueue = true;
            let response = queue.shift();
            if (!response) {
                isProcessingQueue = false;
                return;
            }

            loader.classList.remove('active');
            responseContainer.classList.add('active');
            
            const jsonStringLC = JSON.stringify(response).toLowerCase();
            responseContainer.classList.remove('success', 'failed', 'other');

            if (jsonStringLC.includes('success')) {
                responseContainer.classList.add('success');
                responseTitle.textContent = "Status: Success ✅";
            } else if (jsonStringLC.includes('fail')) {
                responseContainer.classList.add('failed');
                responseTitle.textContent = "Status: Fail ❌";
            } else {
                responseContainer.classList.add('other');
                responseTitle.textContent = "Status: Update Received ℹ️";
            }

            // Always display the event payload consistently
            document.getElementById('responseContent').textContent = JSON.stringify(response, null, 2);
            
            // Continue processing the rest of the current batch of messages
            setTimeout(() => processResponseQueue(queue), 800);
        }

        async function checkWebhook() {
            if (isProcessingQueue) return;

            try {
                const response = await fetch('/webhook-response');
                const data = await response.json();
                
                if (data.responses && data.responses.length > 0) {
                    processResponseQueue(data.responses);
                }
            } catch (error) {
                clearInterval(pollingInterval); 
                errorDiv.textContent = 'Webhook check error: ' + error.message;
                errorDiv.style.display = 'block';
                loader.classList.remove('active');
                formContainer.classList.remove('hidden');
            }
        }

        document.getElementById('tryAgain').addEventListener('click', async () => {
            // This is now the only place polling is stopped by the user
            if (pollingInterval) clearInterval(pollingInterval);
            isProcessingQueue = false;
            
            try {
                await fetch('/reset', { method: 'POST' });
                responseContainer.classList.remove('active', 'success', 'failed', 'other');
                formContainer.classList.remove('hidden');
                errorDiv.textContent = '';
                errorDiv.style.display = 'none';
            } catch (error) {
                errorDiv.textContent = 'Reset error: ' + error.message;
                errorDiv.style.display = 'block';
            }
        });
    </script>
</body>
</html>
"""

# Default JSON payload structure
# default_payload = {
#     "transactionReference": "Successful",
#     "limitType": "Monthly",
#     "accountNumber": "1312134",
#     "cardNumber": "123222",
#     "webhookUrl": "http://localhost:5000/webhook"
# }
default_payload = {
    "idempotencyKey": "6308efd6-cd45-44f2-86f6-ffd6f6ae6a06",
    "transactionReference": "REF-V2-ASYNC-REALTIME-1756329970",
    "modeOfTransfer": "ASYNC",
    "realtime": "true",
    "webhookUrl": "http://localhost:5000/webhook",
    "limitType": "Daily",
    "accountNumber": "1312134",
    "cardNumber": "123111"
}

# --- UPDATED: Worker function now queues ONLY the status of the final response ---
def make_long_running_request(url, method, payload):
    """This function runs in a separate thread and queues only the status string of the final response."""
    message_to_queue = None
    try:
        print(f"BACKGROUND THREAD: Starting request to {url}")
        response = requests.request(method, url, json=payload, timeout=30)
        response.raise_for_status()
        print(f"BACKGROUND THREAD: Request completed with status {response.status_code}")

        try:
            response_body = response.json()
            # As per requirement, queue ONLY the value of the 'status' key
            message_to_queue = response_body.get('status', 'COMPLETED_BUT_STATUS_KEY_MISSING')
        except json.JSONDecodeError:
            message_to_queue = "COMPLETED_BUT_RESPONSE_NOT_JSON"

    except requests.exceptions.RequestException as e:
        print(f"BACKGROUND THREAD ERROR: {e}")
        # Create a simple failure string to be queued
        message_to_queue = f"REQUEST_FAILED: {e.response.status_code if e.response else 'No Response'}"

    with lock:
        webhook_responses.append(message_to_queue)
        print(f"Queued final HTTP status: '{message_to_queue}'")


@app.route('/')
def index():
    with lock:
        webhook_responses.clear()
    
    payload_for_template = default_payload.copy()
    payload_for_template['webhookUrl'] = request.host_url + 'webhook'
    
    return render_template_string(HTML_TEMPLATE, default_payload=json.dumps(payload_for_template, indent=4))

@app.route('/submit', methods=['POST'])
def submit():
    with lock:
        webhook_responses.clear()
    try:
        data = request.get_json()
        payload = json.loads(data['payload'])
        url = data['url']
        method = data['method']
        
        thread = Thread(target=make_long_running_request, args=(url, method, payload))
        thread.daemon = True
        thread.start()
        
        return jsonify({"status": "accepted", "message": "Long-running job started in the background."})

    except Exception as e:
        return jsonify({"error": f"Failed to start job: {str(e)}"}), 500


@app.route('/webhook', methods=['POST', 'PUT'])
def webhook():
    with lock:
        if request.is_json:
            # As per requirement, queue the ENTIRE payload for webhooks
            webhook_responses.append(request.get_json())
            print(f"Queued webhook: {webhook_responses[-1]}")
            return jsonify({"status": "webhook received"}), 200
        else:
            error_data = {"error": "Received non-JSON data", "data": request.get_data(as_text=True)}
            webhook_responses.append(error_data)
            return jsonify({"error": "Invalid webhook format, expected JSON"}), 400

@app.route('/webhook-response')
def get_webhook_response():
    with lock:
        responses_to_send = webhook_responses[:]
        webhook_responses.clear()
    return jsonify({"responses": responses_to_send})

@app.route('/reset', methods=['POST'])
def reset():
    with lock:
        webhook_responses.clear()
    return jsonify({"status": "reset"})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, threaded=True)