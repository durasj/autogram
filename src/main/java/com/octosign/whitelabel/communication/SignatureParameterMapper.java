package com.octosign.whitelabel.communication;

import com.google.common.collect.ImmutableMap;
import eu.europa.esig.dss.AbstractSignatureParameters;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.pades.PAdESSignatureParameters;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import java.util.Map;

import static com.octosign.whitelabel.communication.SignatureParameters.CanonicalizationMethod.EXCLUSIVE;
import static com.octosign.whitelabel.communication.SignatureParameters.CanonicalizationMethod.INCLUSIVE;
import static com.octosign.whitelabel.communication.SignatureParameters.Container.ASICE;
import static com.octosign.whitelabel.communication.SignatureParameters.Container.ASICS;
import static com.octosign.whitelabel.communication.SignatureParameters.DigestAlgorithm.*;
import static com.octosign.whitelabel.communication.SignatureParameters.Level.*;
import static com.octosign.whitelabel.communication.SignatureParameters.Packaging.*;

public class SignatureParameterMapper {
    private static final Map<SignatureParameters.Level, SignatureLevel> signatureLevelMapping =
        Map.of(
            XADES_BASELINE_B, SignatureLevel.XAdES_BASELINE_B,
            XADES_BASELINE_T, SignatureLevel.XAdES_BASELINE_T,
            XADES_BASELINE_LT, SignatureLevel.XAdES_BASELINE_LT,
            XADES_BASELINE_LTA, SignatureLevel.XAdES_BASELINE_LTA,

            PADES_BASELINE_B, SignatureLevel.PAdES_BASELINE_B,
            PADES_BASELINE_T, SignatureLevel.PAdES_BASELINE_T,
            PADES_BASELINE_LT, SignatureLevel.PAdES_BASELINE_LT,
            PADES_BASELINE_LTA, SignatureLevel.PAdES_BASELINE_LTA
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
            ENVELOPING, SignaturePackaging.ENVELOPING,
            DETACHED, SignaturePackaging.DETACHED,
            INTERNALLY_DETACHED, SignaturePackaging.INTERNALLY_DETACHED
        );

    private static final Map<SignatureParameters.CanonicalizationMethod, String> canonicalizationMethodMapping =
        ImmutableMap.of(
            EXCLUSIVE, CanonicalizationMethod.EXCLUSIVE,
            INCLUSIVE, CanonicalizationMethod.INCLUSIVE
        );

    public static SignatureLevel map(SignatureParameters.Level level) { return signatureLevelMapping.get(level); }
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
