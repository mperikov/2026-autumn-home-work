package company.vk.edu.distrib.compute.mperikov.urlshortener;

import java.io.IOException;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import company.vk.edu.distrib.compute.Dao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class UrlShortenerHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerHandler.class);

    private final BasicAuthenticator authenticator;
    private final LinkRequests links;

    UrlShortenerHandler(int port, Dao<String> links, Dao<String> users) {
        authenticator = new BasicAuthenticator(users);
        this.links = new LinkRequests(port, links);
    }

    @Override
    public void handle(HttpExchange exchange) {
        try (exchange) {
            try {
                route(exchange);
            } catch (Exception ex) {
                log.warn("Failed to handle {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), ex);
                HttpReplies.sendServerError(exchange);
            }
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = HttpReplies.requestPath(exchange);
        if (!RequestChecks.isPublic(method, path) && !authenticator.authorized(exchange)) {
            HttpReplies.sendUnauthorized(exchange);
            return;
        }
        if ("GET".equals(method) && RequestChecks.STATUS_PATH.equals(path)) {
            HttpReplies.sendEmpty(exchange, HttpStatus.OK);
            return;
        }
        if ("POST".equals(method) && RequestChecks.USERS_PATH.equals(path)) {
            authenticator.createUser(exchange);
            return;
        }
        if ("POST".equals(method) && RequestChecks.LINKS_PATH.equals(path)) {
            links.create(exchange);
            return;
        }
        if (path.startsWith(RequestChecks.LINKS_PREFIX)) {
            links.handleItem(exchange, method, path.substring(RequestChecks.LINKS_PREFIX.length()));
            return;
        }
        if (RequestChecks.isRedirectPath(method, path)) {
            links.redirect(exchange, path.substring(1));
            return;
        }
        HttpReplies.sendEmpty(exchange, HttpStatus.NOT_FOUND);
    }
}
