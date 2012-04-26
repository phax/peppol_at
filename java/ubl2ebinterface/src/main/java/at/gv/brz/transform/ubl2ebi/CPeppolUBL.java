package at.gv.brz.transform.ubl2ebi;

import javax.annotation.concurrent.Immutable;

import at.peppol.commons.codelist.EInvoiceTypeCode;

@Immutable
public final class CPeppolUBL {
  // The UBL version to use
  public static final String UBL_VERSION = "2.0";

  // The UBL customization to use
  public static final String CUSTOMIZATION_SCHEMEID = "PEPPOL";

  // The invoice type code to use
  public static final String INVOICE_TYPE_CODE = EInvoiceTypeCode.COMMERCIAL_INVOICE.getID ();

  private CPeppolUBL () {}
}
