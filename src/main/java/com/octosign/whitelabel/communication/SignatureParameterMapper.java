package com.octosign.whitelabel.communication;

import com.google.common.collect.ImmutableMap;
import com.octosign.whitelabel.ui.Main;
import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.enumerations.*;
import eu.europa.esig.dss.model.InMemoryDocument;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;
import eu.europa.esig.dss.pades.SignatureFieldParameters;
import eu.europa.esig.dss.pades.SignatureImageParameters;
import eu.europa.esig.dss.pades.SignatureImageTextParameters;
import eu.europa.esig.dss.pdf.pdfbox.visible.PdfBoxNativeFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.awt.*;
import java.util.Map;
import java.util.Objects;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;


import static com.octosign.whitelabel.communication.SignatureParameters.CanonicalizationMethod.EXCLUSIVE;
import static com.octosign.whitelabel.communication.SignatureParameters.CanonicalizationMethod.INCLUSIVE;
import static com.octosign.whitelabel.communication.SignatureParameters.Container.ASICE;
import static com.octosign.whitelabel.communication.SignatureParameters.Container.ASICS;
import static com.octosign.whitelabel.communication.SignatureParameters.DigestAlgorithm.*;
import static com.octosign.whitelabel.communication.SignatureParameters.Format.PADES;
import static com.octosign.whitelabel.communication.SignatureParameters.Format.XADES;
import static com.octosign.whitelabel.communication.SignatureParameters.Level.*;
import static com.octosign.whitelabel.communication.SignatureParameters.Packaging.*;
import static eu.europa.esig.dss.enumerations.SignatureLevel.PAdES_BASELINE_B;
import static eu.europa.esig.dss.enumerations.SignatureLevel.XAdES_BASELINE_B;

public class SignatureParameterMapper {
    private static final Map<SignatureParameters.Level, Map<SignatureParameters.Format, SignatureLevel>> signatureLevelMapping =
        ImmutableMap.of(
            BASELINE_B, ImmutableMap.of(
                XADES, XAdES_BASELINE_B,
                PADES, PAdES_BASELINE_B
            )
        );

    private static final Map<SignatureParameters.Container, ASiCContainerType> asicContainerTypeMapping =
        ImmutableMap.of(
            ASICS, ASiCContainerType.ASiC_S,
            ASICE, ASiCContainerType.ASiC_E
        );

    private static final Map<SignatureParameters.DigestAlgorithm, DigestAlgorithm> digestAlgorithMapping =
        ImmutableMap.of(
            SHA256, DigestAlgorithm.SHA256,
            SHA384, DigestAlgorithm.SHA384,
            SHA512, DigestAlgorithm.SHA512
        );

    private static final Map<SignatureParameters.Packaging, SignaturePackaging> signaturePackagingMapping =
        ImmutableMap.of(
            ENVELOPED, SignaturePackaging.ENVELOPED,
            ENVELOPING, SignaturePackaging.ENVELOPING
        );

    private static final Map<SignatureParameters.CanonicalizationMethod, String> canonicalizationMethodMapping =
        ImmutableMap.of(
            INCLUSIVE, CanonicalizationMethod.INCLUSIVE
        );

    public static SignatureLevel map(SignatureParameters.Level level) { return signatureLevelMapping.get(level).get(XADES); }
    public static ASiCContainerType map(SignatureParameters.Container container) { return asicContainerTypeMapping.get(container); }
    public static DigestAlgorithm map(SignatureParameters.DigestAlgorithm digestAlgorithm) { return digestAlgorithMapping.get(digestAlgorithm); }
    public static SignaturePackaging map(SignatureParameters.Packaging packaging) { return signaturePackagingMapping.get(packaging); }
    public static String map(SignatureParameters.CanonicalizationMethod canonicalizationMethod) { return canonicalizationMethodMapping.get(canonicalizationMethod); }

    public static AbstractSignatureParameters<?> map(SignatureParameters source) {
        return switch (source.getFormat()) {
            case XADES: yield buildXAdESParameters(source);
            case PADES: yield buildPAdESParameters(source);

            default: throw new AssertionError();
        };
    }

    private static ASiCWithXAdESSignatureParameters buildXAdESParameters(SignatureParameters sp) {
        var parameters = new ASiCWithXAdESSignatureParameters();

        parameters.aSiC().setContainerType(map(sp.getContainer()));

        parameters.setSignatureLevel(map(sp.getLevel()));
        parameters.setSignaturePackaging(map(sp.getPackaging()));
        parameters.setDigestAlgorithm(map(sp.getDigestAlgorithm()));
        parameters.setSigningCertificateDigestMethod(map(sp.getDigestAlgorithm()));
        parameters.setSignedInfoCanonicalizationMethod(map(sp.getInfoCanonicalization()));
        parameters.setSignedPropertiesCanonicalizationMethod(map(sp.getPropertiesCanonicalization()));

        // parameters.aSiC().setMimeType(sp.getFileMimeType());
        parameters.setEn319132(sp.isEn319132());

        return parameters;
    }

    private static PAdESSignatureParameters buildPAdESParameters(SignatureParameters sp) {
        var parameters = new PAdESSignatureParameters();

        parameters.setSignatureLevel(map(sp.getLevel()));
        parameters.setDigestAlgorithm(map(sp.getDigestAlgorithm()));
        parameters.setSignaturePackaging(map(sp.getPackaging()));

        return parameters;
    }
}
