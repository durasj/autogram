package digital.slovensko.autogram.core;

import eu.europa.esig.dss.model.DSSDocument;

public abstract class Responder {
    abstract public void onDocumentSigned(DSSDocument r);

    abstract public void onDocumentSignFailed(SigningJob job, SigningError error);
}