package org.bouncycastle.asn1.x509.qualified;

import org.bouncycastle.asn1.DERObjectIdentifier;

public interface ETSIQCObjectIdentifiers
{
    //
    // base id
    //
    static final String                 id_etsi_qcs                  = "0.4.0.1862.1";

    static final DERObjectIdentifier    id_etsi_qcs_QcCompliance     = new DERObjectIdentifier(id_etsi_qcs+".1");
    static final DERObjectIdentifier    id_etsi_qcs_LimiteValue      = new DERObjectIdentifier(id_etsi_qcs+".2");
    static final DERObjectIdentifier    id_etsi_qcs_RetentionPeriod  = new DERObjectIdentifier(id_etsi_qcs+".3");
    static final DERObjectIdentifier    id_etsi_qcs_QcSSCD           = new DERObjectIdentifier(id_etsi_qcs+".4");
}
