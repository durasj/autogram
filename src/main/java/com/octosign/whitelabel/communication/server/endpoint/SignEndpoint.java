package com.octosign.whitelabel.communication.server.endpoint;

import com.octosign.whitelabel.communication.*;
import com.octosign.whitelabel.communication.document.Document;
import com.octosign.whitelabel.communication.document.PDFDocument;
import com.octosign.whitelabel.communication.document.XMLDocument;
import com.octosign.whitelabel.communication.server.Request;
import com.octosign.whitelabel.communication.server.Response;
import com.octosign.whitelabel.communication.server.Server;
import com.octosign.whitelabel.error_handling.Code;
import com.octosign.whitelabel.error_handling.IntegrationException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.concurrent.Future;
import java.util.function.Function;

import static com.google.common.base.CaseFormat.LOWER_HYPHEN;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.octosign.whitelabel.ui.I18n.translate;

public class SignEndpoint extends WriteEndpoint<SignRequest, Document> {

    private Function<SignatureUnit, Future<Document>> onSign;

    public SignEndpoint(Server server, int initialNonce) {
        super(server, initialNonce);
    }

    public void setOnSign(Function<SignatureUnit, Future<Document>> onSign) {
        this.onSign = onSign;
    }

    @Override
    protected Response<Document> handleRequest(Request<SignRequest> request, Response<Document> response) throws IntegrationException {
        if (onSign == null) {
            var error = new CommunicationError(Code.NOT_READY, translate("error.serverNotReady"));
            var errorResponse = new Response<CommunicationError>(request.getExchange())
                    .asError(HttpURLConnection.HTTP_CONFLICT, error);

            send(errorResponse);
            return null;
        }

        var signRequest = request.getBody();
        var document = getSpecificDocument(signRequest);
//        var template = extractTemplateFrom(request);
        var parameters = resolveParameters(signRequest, null);
        var signatureUnit = new SignatureUnit(document, parameters);

        try {
            var signedDocument = onSign.apply(signatureUnit).get();
            return response.setBody(signedDocument);
        } catch (Exception e) {
            // TODO: We should do a better job with the error response here:
            // We can differentiate between application errors (500), user errors (502), missing certificate/UI closed (503)
            var error = new CommunicationError(Code.SIGNING_FAILED, translate("error.signingFailed"), e.getMessage());
            var errorResponse = new Response<CommunicationError>(request.getExchange())
                    .asError(HttpURLConnection.HTTP_INTERNAL_ERROR, error);

            send(errorResponse);
            return null;
        }
    }

    @Override
    protected Class<SignRequest> getRequestClass() {
        return SignRequest.class;
    }

    @Override
    protected Class<Document> getResponseClass() {
        return Document.class;
    }

    @Override
    protected String[] getAllowedMethods() { return new String[] { "POST" }; }

    /**
     * Creates and prepares payload type specific document
     *
     * TODO: Consider extracting this out as this shouldn't be specific to server mode
     *
     * @param signRequest object representing particular signing request data and params
     * @return Specific document like XMLDocument type-widened to Document
     */
    private static Document getSpecificDocument(SignRequest signRequest) throws IntegrationException {
        var document = signRequest.getDocument();
        var parameters = signRequest.getParameters();
        var mimeType = MimeType.parse(signRequest.getPayloadMimeType());

        if (mimeType.equalsTypeSubtype(MimeType.XML)) {
            return buildXMLDocument(document, parameters, mimeType);
        } else if(mimeType.equalsTypeSubtype(MimeType.PDF)) {
            return new PDFDocument(document);
        } else {
            throw new IntegrationException(Code.MALFORMED_MIMETYPE, translate("error.invalidMimetype_", mimeType));
        }
    }

    private static XMLDocument buildXMLDocument(Document document, SignatureParameters parameters, MimeType mimeType) throws IntegrationException {
        var schema = parameters.getSchema();
        var transformation = parameters.getTransformation();

        if (mimeType.isBase64()) {
            try {
                document.setContent(decode(document.getContent()));
                schema = decode(schema);
                transformation = decode(transformation);
            } catch (IllegalArgumentException e) {
                throw new IntegrationException(Code.DECODING_FAILED, e);
            }
        }
        return new XMLDocument(document, schema, transformation);
    }

    private static String decode(String input) {
        if (input == null || input.isBlank()) return null;

        var decoder = Base64.getDecoder();
        return new String(decoder.decode(input));
    }

    private static Configuration extractTemplateFrom(Request<?> request) {
        var templateId = request.getQueryParams().get("template");
        if (templateId == null || templateId.isEmpty()) return null;

        var templateName = LOWER_HYPHEN.to(UPPER_UNDERSCORE, templateId);
        return Configuration.from(templateName);
    }

    private static SignatureParameters resolveParameters(SignRequest signRequest, Configuration template) {

        return signRequest.getParameters();

//        var sourceParams = (template != null) ? template.parameters() : signRequest.getParameters();
//
//        return new SignatureParameters.Builder(sourceParams)
//                .schema(sourceParams.getSchema())
//                .transformation(sourceParams.getTransformation())
//                .signaturePolicyId(sourceParams.getSignaturePolicyId())
//                .signaturePolicyContent(sourceParams.getSignaturePolicyContent())
//                .build();
    }
}
