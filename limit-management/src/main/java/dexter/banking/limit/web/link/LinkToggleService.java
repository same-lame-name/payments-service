package dexter.banking.limit.web.link;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LinkToggleService {

    @Value("${jsonapi.links.enabled:true}")
    private boolean linksEnabled;

    public boolean isLinksEnabled() {
        return linksEnabled;
    }
}