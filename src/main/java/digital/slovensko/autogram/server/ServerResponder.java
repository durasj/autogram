package digital.slovensko.autogram.server;

import com.sun.net.httpserver.HttpExchange;

import digital.slovensko.autogram.core.Responder;
import digital.slovensko.autogram.core.SignedDocument;
import digital.slovensko.autogram.core.SigningError;
import digital.slovensko.autogram.core.SigningJob;

import java.io.IOException;
import java.util.Base64;

public class ServerResponder extends Responder {
    private final HttpExchange exchange;

    public ServerResponder(HttpExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void onDocumentSigned(SignedDocument signedDocument) {
        var signer = signedDocument.getCertificate().getSubject().getPrincipal().toString();
        var issuer = signedDocument.getCertificate().getIssuer().getPrincipal().toString();

        try {
            var b64document = Base64.getEncoder()
                    .encodeToString(signedDocument.getDocument().openStream().readAllBytes());
            var msg = String.format("{\"content\": \"%s\", \"signedBy\": \"%s\", \"issuedBy\": \"%s\"}", b64document,
                    signer, issuer);

            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(msg.getBytes());
            exchange.getResponseBody().close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDocumentSignFailed(SigningJob job, SigningError error) {
        try {
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
