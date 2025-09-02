# listener.py
from flask import Flask, request
from datetime import datetime, timezone
import sys

app = Flask(__name__)

@app.route('/webhook-receiver', methods=['POST'])
def webhook_receiver():
    """
    Receives webhook notifications, logs the end time and reference,
    and returns a 200 OK response.
    """
    try:
        # Get the current time in UTC ISO 8601 format
        end_time = datetime.now(timezone.utc).isoformat()
        
        # Get the JSON payload sent by the book-transfers-service
        payload = request.get_json()

        # Extract the transaction reference to correlate with the start time
        # NOTE: Ensure your service includes 'transactionReference' in the webhook body
        transaction_reference = payload.get('transactionReference')

        if not transaction_reference:
            # Log an error if the reference is missing
            print(f"ERROR,{end_time},Missing transactionReference in payload", file=sys.stderr)
            return "Missing transactionReference", 400

        # Log in a machine-readable format to standard output
        # Format: STATUS,TIMESTAMP,REFERENCE
        print(f"END,{end_time},{transaction_reference}")
        
        return "OK", 200
    except Exception as e:
        # Log any unexpected errors
        print(f"ERROR,{datetime.now(timezone.utc).isoformat()},{e}", file=sys.stderr)
        return "Internal Server Error", 500

if __name__ == '__main__':
    # Run the Flask app on port 3000
    app.run(port=3000)