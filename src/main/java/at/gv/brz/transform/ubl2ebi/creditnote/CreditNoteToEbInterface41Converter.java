/**
 * Copyright (C) 2010-2014 Bundesrechenzentrum GmbH
 * http://www.brz.gv.at
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.gv.brz.transform.ubl2ebi.creditnote;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import javax.xml.datatype.XMLGregorianCalendar;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CreditNoteLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.DeliveryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.OrderLineReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.OrderReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PeriodType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.DescriptionType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NameType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NoteType;
import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import at.gv.brz.transform.ubl2ebi.EbInterface41Helper;
import at.gv.brz.transform.ubl2ebi.helper.SchemedID;
import at.gv.brz.transform.ubl2ebi.helper.TaxCategoryKey;

import com.phloc.commons.CGlobal;
import com.phloc.commons.collections.ContainerHelper;
import com.phloc.commons.math.MathHelper;
import com.phloc.commons.state.ETriState;
import com.phloc.commons.string.StringHelper;
import com.phloc.commons.string.StringParser;
import com.phloc.ebinterface.codelist.ETaxCode;
import com.phloc.ebinterface.v41.Ebi41AddressType;
import com.phloc.ebinterface.v41.Ebi41BillerType;
import com.phloc.ebinterface.v41.Ebi41CurrencyType;
import com.phloc.ebinterface.v41.Ebi41DeliveryType;
import com.phloc.ebinterface.v41.Ebi41DetailsType;
import com.phloc.ebinterface.v41.Ebi41DocumentTypeType;
import com.phloc.ebinterface.v41.Ebi41FurtherIdentificationType;
import com.phloc.ebinterface.v41.Ebi41InvoiceRecipientType;
import com.phloc.ebinterface.v41.Ebi41InvoiceType;
import com.phloc.ebinterface.v41.Ebi41ItemListType;
import com.phloc.ebinterface.v41.Ebi41ListLineItemType;
import com.phloc.ebinterface.v41.Ebi41NoPaymentType;
import com.phloc.ebinterface.v41.Ebi41OrderReferenceDetailType;
import com.phloc.ebinterface.v41.Ebi41OrderReferenceType;
import com.phloc.ebinterface.v41.Ebi41OtherTaxType;
import com.phloc.ebinterface.v41.Ebi41PaymentConditionsType;
import com.phloc.ebinterface.v41.Ebi41PaymentMethodType;
import com.phloc.ebinterface.v41.Ebi41PeriodType;
import com.phloc.ebinterface.v41.Ebi41ReductionAndSurchargeBaseType;
import com.phloc.ebinterface.v41.Ebi41ReductionAndSurchargeDetailsType;
import com.phloc.ebinterface.v41.Ebi41ReductionAndSurchargeListLineItemDetailsType;
import com.phloc.ebinterface.v41.Ebi41ReductionAndSurchargeType;
import com.phloc.ebinterface.v41.Ebi41TaxType;
import com.phloc.ebinterface.v41.Ebi41UnitPriceType;
import com.phloc.ebinterface.v41.Ebi41UnitType;
import com.phloc.ebinterface.v41.Ebi41VATItemType;
import com.phloc.ebinterface.v41.Ebi41VATRateType;
import com.phloc.ebinterface.v41.Ebi41VATType;
import com.phloc.ebinterface.v41.ObjectFactory;
import com.phloc.ubl21.codelist.EUnitOfMeasureCode21;
import com.phloc.validation.error.ErrorList;

import eu.europa.ec.cipa.peppol.codelist.ETaxSchemeID;

/**
 * Main converter between UBL 2.1 credit note and ebInterface 4.1 credit note.
 * 
 * @author philip
 */
@Immutable
public final class CreditNoteToEbInterface41Converter extends AbstractCreditNoteConverter
{
  /**
   * Constructor
   * 
   * @param aDisplayLocale
   *        The locale for error messages. May not be <code>null</code>.
   * @param aContentLocale
   *        The locale for the created ebInterface files. May not be
   *        <code>null</code>.
   * @param bStrictERBMode
   *        <code>true</code> if ER>B specific checks should be performed
   */
  public CreditNoteToEbInterface41Converter (@Nonnull final Locale aDisplayLocale,
                                             @Nonnull final Locale aContentLocale,
                                             final boolean bStrictERBMode)
  {
    super (aDisplayLocale, aContentLocale, bStrictERBMode);
  }

  /**
   * Main conversion method to convert from UBL to ebInterface 4.1
   * 
   * @param aUBLDoc
   *        The UBL invoice to be converted
   * @param aTransformationErrorList
   *        Error list. Must be empty!
   * @return The created ebInterface 4.1 document or <code>null</code> in case
   *         of a severe error.
   */
  @Nullable
  public Ebi41InvoiceType convertToEbInterface (@Nonnull final CreditNoteType aUBLDoc,
                                                @Nonnull final ErrorList aTransformationErrorList)
  {
    if (aUBLDoc == null)
      throw new NullPointerException ("UBLCreditNote");
    if (aTransformationErrorList == null)
      throw new NullPointerException ("TransformationErrorList");
    if (!aTransformationErrorList.isEmpty ())
      throw new IllegalArgumentException ("TransformationErrorList must be empty!");

    // Consistency check before starting the conversion
    _checkConsistency (aUBLDoc, aTransformationErrorList);
    if (aTransformationErrorList.containsAtLeastOneError ())
      return null;

    // Build ebInterface invoice
    final Ebi41InvoiceType aEbiDoc = new Ebi41InvoiceType ();
    aEbiDoc.setGeneratingSystem (EBI_GENERATING_SYSTEM_41);
    aEbiDoc.setDocumentType (Ebi41DocumentTypeType.CREDIT_MEMO);

    // Cannot set the language, because the 3letter code is expected but we only
    // have the 2letter code!

    final String sUBLCurrencyCode = StringHelper.trim (aUBLDoc.getDocumentCurrencyCodeValue ());
    try
    {
      aEbiDoc.setInvoiceCurrency (Ebi41CurrencyType.fromValue (sUBLCurrencyCode));
    }
    catch (final Exception ex)
    {
      aTransformationErrorList.addError ("DocumentCurrencyCode",
                                         EText.INVALID_CURRENCY_CODE.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                             sUBLCurrencyCode));
    }

    // CreditNote Number
    final String sCreditNoteNumber = StringHelper.trim (aUBLDoc.getIDValue ());
    if (StringHelper.hasNoText (sCreditNoteNumber))
      aTransformationErrorList.addError ("ID", EText.MISSING_INVOICE_NUMBER.getDisplayText (m_aDisplayLocale));
    aEbiDoc.setInvoiceNumber (sCreditNoteNumber);

    // Ignore the time!
    aEbiDoc.setInvoiceDate (aUBLDoc.getIssueDateValue ());
    if (aEbiDoc.getInvoiceDate () == null)
      aTransformationErrorList.addError ("IssueDate", EText.MISSING_INVOICE_DATE.getDisplayText (m_aDisplayLocale));

    // Is duplicate/copy indicator?
    if (aUBLDoc.getCopyIndicator () != null)
      aEbiDoc.setIsDuplicate (Boolean.valueOf (aUBLDoc.getCopyIndicator ().isValue ()));

    // Global comment
    {
      final List <String> aEbiComment = new ArrayList <String> ();
      for (final NoteType aNote : aUBLDoc.getNote ())
        if (StringHelper.hasText (aNote.getValue ()))
          aEbiComment.add (aNote.getValue ());
      if (!aEbiComment.isEmpty ())
        aEbiDoc.setComment (StringHelper.getImplodedNonEmpty ('\n', aEbiComment));
    }

    // Biller/Supplier (creator of the invoice)
    {
      final SupplierPartyType aUBLSupplier = aUBLDoc.getAccountingSupplierParty ();
      final Ebi41BillerType aEbiBiller = new Ebi41BillerType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLSupplier.getParty ().getPartyTaxScheme ())
        if (SUPPORTED_TAX_SCHEME_ID.getID ().equals (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ()))
        {
          aEbiBiller.setVATIdentificationNumber (StringHelper.trim (aUBLPartyTaxScheme.getCompanyIDValue ()));
          break;
        }
      if (StringHelper.hasNoText (aEbiBiller.getVATIdentificationNumber ()))
      {
        // Required by ebInterface 4.0
        aTransformationErrorList.addError ("AccountingSupplierParty/Party/PartyTaxScheme",
                                           EText.BILLER_VAT_MISSING.getDisplayText (m_aDisplayLocale));
      }
      if (aUBLSupplier.getCustomerAssignedAccountID () != null)
      {
        // The customer's internal identifier for the supplier.
        aEbiBiller.setInvoiceRecipientsBillerID (StringHelper.trim (aUBLSupplier.getCustomerAssignedAccountIDValue ()));
      }

      // Disabled because field is optional
      if (false)
        if (m_bStrictERBMode && StringHelper.hasNoText (aEbiBiller.getInvoiceRecipientsBillerID ()))
        {
          // Mandatory field
          aTransformationErrorList.addError ("AccountingSupplierParty/CustomerAssignedAccountID",
                                             EText.ERB_CUSTOMER_ASSIGNED_ACCOUNTID_MISSING.getDisplayText (m_aDisplayLocale));
        }
      aEbiBiller.setAddress (EbInterface41Helper.convertParty (aUBLSupplier.getParty (),
                                                               "AccountingSupplierParty",
                                                               aTransformationErrorList,
                                                               m_aContentLocale,
                                                               m_aDisplayLocale));
      aEbiDoc.setBiller (aEbiBiller);
    }

    // CreditNote recipient
    {
      final CustomerPartyType aUBLCustomer = aUBLDoc.getAccountingCustomerParty ();
      final Ebi41InvoiceRecipientType aEbiRecipient = new Ebi41InvoiceRecipientType ();
      // Find the tax scheme that uses VAT
      for (final PartyTaxSchemeType aUBLPartyTaxScheme : aUBLCustomer.getParty ().getPartyTaxScheme ())
        if (SUPPORTED_TAX_SCHEME_ID.getID ().equals (aUBLPartyTaxScheme.getTaxScheme ().getIDValue ()))
        {
          aEbiRecipient.setVATIdentificationNumber (StringHelper.trim (aUBLPartyTaxScheme.getCompanyIDValue ()));
          break;
        }
      if (StringHelper.hasNoText (aEbiRecipient.getVATIdentificationNumber ()))
      {
        // Required by ebInterface 4.0
        aTransformationErrorList.addError ("AccountingCustomerParty/PartyTaxScheme",
                                           EText.SUPPLIER_VAT_MISSING.getDisplayText (m_aDisplayLocale));
      }
      if (aUBLCustomer.getSupplierAssignedAccountID () != null)
      {
        // UBL: An identifier for the Customer's account, assigned by the
        // Supplier.
        // eb: Identifikation des Rechnungsempfängers beim Rechnungssteller.
        final String sBillersInvoiceRecipientID = StringHelper.trim (aUBLCustomer.getSupplierAssignedAccountIDValue ());
        aEbiRecipient.setBillersInvoiceRecipientID (sBillersInvoiceRecipientID);
      }
      if (StringHelper.hasNoText (aEbiRecipient.getBillersInvoiceRecipientID ()))
      {
        // Mandatory field
        aTransformationErrorList.addWarning ("AccountingCustomerParty/SupplierAssignedAccountID",
                                             EText.SUPPLIER_ASSIGNED_ACCOUNTID_MISSING.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                               DEFAULT_BILLERS_INVOICERECIPIENT_ID));
        aEbiRecipient.setBillersInvoiceRecipientID (DEFAULT_BILLERS_INVOICERECIPIENT_ID);
      }
      aEbiRecipient.setAddress (EbInterface41Helper.convertParty (aUBLCustomer.getParty (),
                                                                  "AccountingCustomerParty",
                                                                  aTransformationErrorList,
                                                                  m_aContentLocale,
                                                                  m_aDisplayLocale));
      aEbiDoc.setInvoiceRecipient (aEbiRecipient);
    }

    // Order reference of invoice recipient
    String sUBLOrderReferenceID = null;
    {
      final OrderReferenceType aUBLOrderReference = aUBLDoc.getOrderReference ();
      if (aUBLOrderReference != null)
      {
        // Use directly from order reference
        sUBLOrderReferenceID = StringHelper.trim (aUBLOrderReference.getIDValue ());
      }

      if (StringHelper.hasNoText (sUBLOrderReferenceID))
      {
        aTransformationErrorList.addError ("OrderReference/ID",
                                           EText.ORDER_REFERENCE_MISSING.getDisplayText (m_aDisplayLocale));
      }
      else
      {
        if (sUBLOrderReferenceID != null && sUBLOrderReferenceID.length () > ORDER_REFERENCE_MAX_LENGTH)
        {
          aTransformationErrorList.addWarning ("OrderReference/ID",
                                               EText.ORDER_REFERENCE_TOO_LONG.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      sUBLOrderReferenceID,
                                                                                                      Integer.valueOf (ORDER_REFERENCE_MAX_LENGTH)));
          sUBLOrderReferenceID = sUBLOrderReferenceID.substring (0, ORDER_REFERENCE_MAX_LENGTH);
        }
      }

      final Ebi41OrderReferenceType aEbiOrderReference = new Ebi41OrderReferenceType ();
      aEbiOrderReference.setOrderID (sUBLOrderReferenceID);
      aEbiDoc.getInvoiceRecipient ().setOrderReference (aEbiOrderReference);

      // Add contract reference as further identification
      for (final DocumentReferenceType aDocumentReference : aUBLDoc.getContractDocumentReference ())
        if (StringHelper.hasTextAfterTrim (aDocumentReference.getIDValue ()))
        {
          final Ebi41FurtherIdentificationType aEbiFurtherIdentification = new Ebi41FurtherIdentificationType ();
          aEbiFurtherIdentification.setIdentificationType ("Contract");
          aEbiFurtherIdentification.setValue (StringHelper.trim (aDocumentReference.getIDValue ()));
          aEbiDoc.getInvoiceRecipient ().getFurtherIdentification ().add (aEbiFurtherIdentification);
        }
    }

    // Tax totals
    // Map from tax category to percentage
    final Map <TaxCategoryKey, BigDecimal> aTaxCategoryPercMap = new HashMap <TaxCategoryKey, BigDecimal> ();
    final Ebi41TaxType aEbiTax = new Ebi41TaxType ();
    final Ebi41VATType aEbiVAT = new Ebi41VATType ();
    {
      int nTaxTotalIndex = 0;
      for (final TaxTotalType aUBLTaxTotal : aUBLDoc.getTaxTotal ())
      {
        int nTaxSubtotalIndex = 0;
        for (final TaxSubtotalType aUBLSubtotal : aUBLTaxTotal.getTaxSubtotal ())
        {
          // Tax category is a mandatory element
          final TaxCategoryType aUBLTaxCategory = aUBLSubtotal.getTaxCategory ();
          BigDecimal aUBLTaxAmount = aUBLSubtotal.getTaxAmountValue ();
          BigDecimal aUBLTaxableAmount = aUBLSubtotal.getTaxableAmountValue ();

          // Is the percentage value directly specified
          BigDecimal aUBLPercentage = aUBLTaxCategory.getPercentValue ();
          if (aUBLPercentage == null)
          {
            // no it is not :(
            if (aUBLTaxAmount != null && aUBLTaxableAmount != null)
            {
              // Calculate percentage
              aUBLPercentage = MathHelper.isEqualToZero (aUBLTaxableAmount) ? BigDecimal.ZERO
                                                                           : aUBLTaxAmount.multiply (CGlobal.BIGDEC_100)
                                                                                          .divide (aUBLTaxableAmount,
                                                                                                   SCALE_PERC,
                                                                                                   ROUNDING_MODE);
            }
          }

          if (aUBLPercentage != null)
          {
            // We have at least the percentage
            if (aUBLTaxableAmount == null && aUBLTaxAmount != null)
            {
              // Cannot "back" calculate the taxable amount from 0 percentage!
              if (MathHelper.isNotEqualToZero (aUBLPercentage))
              {
                // Calculate (inexact) subtotal
                aUBLTaxableAmount = aUBLTaxAmount.multiply (CGlobal.BIGDEC_100).divide (aUBLPercentage,
                                                                                        SCALE_PRICE_LINE,
                                                                                        ROUNDING_MODE);
              }
            }
            else
              if (aUBLTaxableAmount != null && aUBLTaxAmount == null)
              {
                // Calculate (inexact) subtotal
                aUBLTaxAmount = MathHelper.isEqualToZero (aUBLPercentage) ? BigDecimal.ZERO
                                                                         : aUBLTaxableAmount.multiply (aUBLPercentage)
                                                                                            .divide (CGlobal.BIGDEC_100,
                                                                                                     SCALE_PRICE_LINE,
                                                                                                     ROUNDING_MODE);
              }
          }

          // Save item and put in map
          final String sUBLTaxSchemeSchemeID = StringHelper.trim (aUBLTaxCategory.getTaxScheme ()
                                                                                 .getID ()
                                                                                 .getSchemeID ());
          final String sUBLTaxSchemeID = StringHelper.trim (aUBLTaxCategory.getTaxScheme ().getIDValue ());

          if (aUBLTaxCategory.getID () == null)
          {
            aTransformationErrorList.addError ("TaxTotal[" +
                                                   nTaxTotalIndex +
                                                   "]/TaxSubtotal[" +
                                                   nTaxSubtotalIndex +
                                                   "]/TaxCategory",
                                               EText.MISSING_TAXCATEGORY_ID.getDisplayText (m_aDisplayLocale));
            break;
          }

          final String sUBLTaxCategorySchemeID = StringHelper.trim (aUBLTaxCategory.getID ().getSchemeID ());
          final String sUBLTaxCategoryID = StringHelper.trim (aUBLTaxCategory.getID ().getValue ());

          aTaxCategoryPercMap.put (new TaxCategoryKey (new SchemedID (sUBLTaxSchemeSchemeID, sUBLTaxSchemeID),
                                                       new SchemedID (sUBLTaxCategorySchemeID, sUBLTaxCategoryID)),
                                   aUBLPercentage);

          if (isSupportedTaxSchemeSchemeID (sUBLTaxSchemeSchemeID))
          {
            final ETaxSchemeID eUBLTaxScheme = ETaxSchemeID.getFromIDOrNull (sUBLTaxSchemeID);
            if (eUBLTaxScheme == null)
            {
              aTransformationErrorList.addError ("TaxTotal[" +
                                                     nTaxTotalIndex +
                                                     "]/TaxSubtotal[" +
                                                     nTaxSubtotalIndex +
                                                     "]/TaxCategory/TaxScheme/ID",
                                                 EText.UNSUPPORTED_TAX_SCHEME_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                         sUBLTaxSchemeID));
            }
            else
            {
              if (SUPPORTED_TAX_SCHEME_ID.equals (eUBLTaxScheme))
              {
                if (aUBLPercentage == null)
                {
                  aTransformationErrorList.addError ("TaxTotal[" +
                                                         nTaxTotalIndex +
                                                         "]/TaxSubtotal[" +
                                                         nTaxSubtotalIndex +
                                                         "]/TaxCategory/Percent",
                                                     EText.TAX_PERCENT_MISSING.getDisplayTextWithArgs (m_aDisplayLocale));
                }
                else
                {
                  // add VAT item
                  final Ebi41VATItemType aEbiVATItem = new Ebi41VATItemType ();
                  // Base amount
                  aEbiVATItem.setTaxedAmount (aUBLTaxableAmount);
                  // tax rate
                  final Ebi41VATRateType aEbiVATVATRate = new Ebi41VATRateType ();
                  // Optional
                  if (false)
                    aEbiVATVATRate.setTaxCode (sUBLTaxCategoryID);
                  aEbiVATVATRate.setValue (aUBLPercentage);
                  aEbiVATItem.setVATRate (aEbiVATVATRate);
                  // Tax amount (mandatory)
                  aEbiVATItem.setAmount (aUBLTaxAmount);
                  // Add to list
                  aEbiVAT.getVATItem ().add (aEbiVATItem);
                }
              }
              else
              {
                // Other TAX
                final Ebi41OtherTaxType aOtherTax = new Ebi41OtherTaxType ();
                // As no comment is present, use the scheme ID
                aOtherTax.setComment (sUBLTaxSchemeID);
                // Tax amount (mandatory)
                aOtherTax.setAmount (aUBLTaxAmount);
                aEbiTax.getOtherTax ().add (aOtherTax);
              }
            }
          }
          else
          {
            aTransformationErrorList.addError ("TaxTotal[" +
                                                   nTaxTotalIndex +
                                                   "]/TaxSubtotal[" +
                                                   nTaxSubtotalIndex +
                                                   "]/TaxCategory/",
                                               EText.UNSUPPORTED_TAX_SCHEME.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                    sUBLTaxSchemeSchemeID,
                                                                                                    sUBLTaxSchemeID));
          }
          ++nTaxSubtotalIndex;
        }
        ++nTaxTotalIndex;
      }

      aEbiTax.setVAT (aEbiVAT);
      aEbiDoc.setTax (aEbiTax);
    }

    // Line items
    BigDecimal aTotalZeroPercLineExtensionAmount = BigDecimal.ZERO;
    {
      final Ebi41DetailsType aEbiDetails = new Ebi41DetailsType ();
      final Ebi41ItemListType aEbiItemList = new Ebi41ItemListType ();
      int nLineIndex = 0;
      for (final CreditNoteLineType aUBLLine : aUBLDoc.getCreditNoteLine ())
      {
        // Try to resolve tax category
        TaxCategoryType aUBLTaxCategory = ContainerHelper.getSafe (aUBLLine.getItem ().getClassifiedTaxCategory (), 0);
        if (aUBLTaxCategory == null)
        {
          // No direct tax category -> check if it is somewhere in the tax total
          outer: for (final TaxTotalType aUBLTaxTotal : aUBLLine.getTaxTotal ())
            for (final TaxSubtotalType aUBLTaxSubTotal : aUBLTaxTotal.getTaxSubtotal ())
            {
              // Only handle VAT items
              if (SUPPORTED_TAX_SCHEME_ID.getID ().equals (aUBLTaxSubTotal.getTaxCategory ()
                                                                          .getTaxScheme ()
                                                                          .getIDValue ()))
              {
                // We found one -> just use it
                aUBLTaxCategory = aUBLTaxSubTotal.getTaxCategory ();
                break outer;
              }
            }
        }

        // Try to resolve tax percentage
        BigDecimal aUBLPercent = null;
        if (aUBLTaxCategory != null)
        {
          // Specified at tax category?
          if (aUBLTaxCategory.getPercent () != null)
            aUBLPercent = aUBLTaxCategory.getPercentValue ();

          if (aUBLPercent == null &&
              aUBLTaxCategory.getID () != null &&
              aUBLTaxCategory.getTaxScheme () != null &&
              aUBLTaxCategory.getTaxScheme ().getID () != null)
          {
            // Not specified - check from previous map
            final String sUBLTaxSchemeSchemeID = StringHelper.trim (aUBLTaxCategory.getTaxScheme ()
                                                                                   .getID ()
                                                                                   .getSchemeID ());
            final String sUBLTaxSchemeID = StringHelper.trim (aUBLTaxCategory.getTaxScheme ().getIDValue ());

            final String sUBLTaxCategorySchemeID = StringHelper.trim (aUBLTaxCategory.getID ().getSchemeID ());
            final String sUBLTaxCategoryID = StringHelper.trim (aUBLTaxCategory.getIDValue ());

            final TaxCategoryKey aKey = new TaxCategoryKey (new SchemedID (sUBLTaxSchemeSchemeID, sUBLTaxSchemeID),
                                                            new SchemedID (sUBLTaxCategorySchemeID, sUBLTaxCategoryID));
            aUBLPercent = aTaxCategoryPercMap.get (aKey);
          }
        }
        if (aUBLPercent == null)
        {
          aUBLPercent = BigDecimal.ZERO;
          aTransformationErrorList.addWarning ("CreditNoteLine[" + nLineIndex + "]/Item/ClassifiedTaxCategory",
                                               EText.DETAILS_TAX_PERCENTAGE_NOT_FOUND.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                              aUBLPercent));
        }

        // Start creating ebInterface line
        final Ebi41ListLineItemType aEbiListLineItem = new Ebi41ListLineItemType ();

        // CreditNote line number
        final String sUBLPositionNumber = StringHelper.trim (aUBLLine.getIDValue ());
        BigInteger aUBLPositionNumber = StringParser.parseBigInteger (sUBLPositionNumber);
        if (aUBLPositionNumber == null)
        {
          aUBLPositionNumber = BigInteger.valueOf (nLineIndex + 1);
          aTransformationErrorList.addWarning ("CreditNoteLine[" + nLineIndex + "]/ID",
                                               EText.DETAILS_INVALID_POSITION.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      sUBLPositionNumber,
                                                                                                      aUBLPositionNumber));
        }
        aEbiListLineItem.setPositionNumber (aUBLPositionNumber);

        // Descriptions
        for (final DescriptionType aUBLDescription : aUBLLine.getItem ().getDescription ())
          aEbiListLineItem.getDescription ().add (StringHelper.trim (aUBLDescription.getValue ()));
        if (aEbiListLineItem.getDescription ().isEmpty ())
        {
          // Use item name as description
          final NameType aUBLName = aUBLLine.getItem ().getName ();
          if (aUBLName != null)
            aEbiListLineItem.getDescription ().add (StringHelper.trim (aUBLName.getValue ()));
        }

        // Quantity
        final Ebi41UnitType aEbiQuantity = new Ebi41UnitType ();
        if (aUBLLine.getCreditedQuantity () != null)
        {
          // Unit code is optional
          if (aUBLLine.getCreditedQuantity ().getUnitCode () != null)
            aEbiQuantity.setUnit (StringHelper.trim (aUBLLine.getCreditedQuantity ().getUnitCode ()));
          aEbiQuantity.setValue (aUBLLine.getCreditedQuantityValue ());
        }
        if (aEbiQuantity.getUnit () == null)
        {
          // ebInterface requires a quantity!
          aEbiQuantity.setUnit (EUnitOfMeasureCode21.C62.getID ());
          aTransformationErrorList.addWarning ("CreditNoteLine[" + nLineIndex + "]/CreditNotedQuantity/UnitCode",
                                               EText.DETAILS_INVALID_UNIT.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                  aEbiQuantity.getUnit ()));
        }
        if (aEbiQuantity.getValue () == null)
        {
          aEbiQuantity.setValue (BigDecimal.ONE);
          aTransformationErrorList.addWarning ("CreditNoteLine[" + nLineIndex + "]/CreditNotedQuantity",
                                               EText.DETAILS_INVALID_QUANTITY.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                      aEbiQuantity.getValue ()));
        }
        aEbiListLineItem.setQuantity (aEbiQuantity);

        // Unit price
        if (aUBLLine.getPrice () != null)
        {
          final Ebi41UnitPriceType aEbiUnitPrice = new Ebi41UnitPriceType ();
          // Unit price = priceAmount/baseQuantity (mandatory)
          final BigDecimal aUBLPriceAmount = aUBLLine.getPrice ().getPriceAmountValue ();
          aEbiUnitPrice.setValue (aUBLPriceAmount);
          // If no base quantity is present, assume 1 (optional)
          final BigDecimal aUBLBaseQuantity = aUBLLine.getPrice ().getBaseQuantityValue ();
          if (aUBLBaseQuantity != null)
          {
            aEbiUnitPrice.setBaseQuantity (aUBLBaseQuantity);
            if (MathHelper.isEqualToZero (aUBLBaseQuantity))
              aEbiUnitPrice.setValue (BigDecimal.ZERO);
          }
          aEbiListLineItem.setUnitPrice (aEbiUnitPrice);
        }
        else
        {
          // Unit price = lineExtensionAmount / quantity (mandatory)
          final BigDecimal aUBLLineExtensionAmount = aUBLLine.getLineExtensionAmountValue ();
          final Ebi41UnitPriceType aEbiUnitPrice = new Ebi41UnitPriceType ();
          if (MathHelper.isEqualToZero (aEbiQuantity.getValue ()))
            aEbiUnitPrice.setValue (BigDecimal.ZERO);
          else
            aEbiUnitPrice.setValue (aUBLLineExtensionAmount.divide (aEbiQuantity.getValue (),
                                                                    SCALE_PRICE_LINE,
                                                                    ROUNDING_MODE));
          aEbiListLineItem.setUnitPrice (aEbiUnitPrice);
        }

        BigDecimal aEbiUnitPriceValue = aEbiListLineItem.getUnitPrice ().getValue ();
        if (aEbiListLineItem.getUnitPrice ().getBaseQuantity () != null)
          aEbiUnitPriceValue = aEbiUnitPriceValue.divide (aEbiListLineItem.getUnitPrice ().getBaseQuantity (),
                                                          SCALE_PRICE_LINE,
                                                          ROUNDING_MODE);

        // Tax rate (mandatory)
        final Ebi41VATRateType aEbiVATRate = new Ebi41VATRateType ();
        aEbiVATRate.setValue (aUBLPercent);
        if (aUBLTaxCategory != null)
          // Optional
          if (false)
            aEbiVATRate.setTaxCode (aUBLTaxCategory.getIDValue ());
        aEbiListLineItem.setVATRate (aEbiVATRate);

        // Line item amount (quantity * unit price +- reduction / surcharge)
        aEbiListLineItem.setLineItemAmount (aUBLLine.getLineExtensionAmountValue ());

        // Special handling in case no VAT item is present
        if (MathHelper.isEqualToZero (aUBLPercent))
          aTotalZeroPercLineExtensionAmount = aTotalZeroPercLineExtensionAmount.add (aUBLLine.getLineExtensionAmountValue ());

        // Order reference per line (UBL 2.1 only)
        for (final OrderLineReferenceType aUBLOrderLineReference : aUBLLine.getOrderLineReference ())
          if (StringHelper.hasText (aUBLOrderLineReference.getLineIDValue ()))
          {
            final Ebi41OrderReferenceDetailType aEbiOrderRefDetail = new Ebi41OrderReferenceDetailType ();

            // order reference
            String sUBLLineOrderReferenceID = null;
            if (aUBLOrderLineReference.getOrderReference () != null)
              sUBLLineOrderReferenceID = StringHelper.trim (aUBLOrderLineReference.getOrderReference ().getIDValue ());
            if (StringHelper.hasNoText (sUBLLineOrderReferenceID))
            {
              // Use the global order reference from header level
              sUBLLineOrderReferenceID = sUBLOrderReferenceID;
            }
            aEbiOrderRefDetail.setOrderID (sUBLLineOrderReferenceID);

            // Order position number
            final String sOrderPosNumber = StringHelper.trim (aUBLOrderLineReference.getLineIDValue ());
            if (sOrderPosNumber != null)
            {
              if (sOrderPosNumber.length () == 0)
              {
                aTransformationErrorList.addError ("CreditNoteLine[" + nLineIndex + "]/OrderLineReference/LineID",
                                                   EText.ORDERLINE_REF_ID_EMPTY.getDisplayText (m_aDisplayLocale));
              }
              else
              {
                aEbiOrderRefDetail.setOrderPositionNumber (sOrderPosNumber);
              }
            }
            aEbiListLineItem.setInvoiceRecipientsOrderReference (aEbiOrderRefDetail);
            break;
          }

        // Reduction and surcharge (UBL 2.1 only)
        if (aUBLLine.hasAllowanceChargeEntries ())
        {
          // Start with quantity*unitPrice for base amount
          BigDecimal aEbiBaseAmount = aEbiListLineItem.getQuantity ().getValue ().multiply (aEbiUnitPriceValue);
          final Ebi41ReductionAndSurchargeListLineItemDetailsType aEbiRSDetails = new Ebi41ReductionAndSurchargeListLineItemDetailsType ();

          // ebInterface can handle only Reduction or only Surcharge
          ETriState eSurcharge = ETriState.UNDEFINED;
          for (final AllowanceChargeType aUBLAllowanceCharge : aUBLLine.getAllowanceCharge ())
          {
            final boolean bItemIsSurcharge = aUBLAllowanceCharge.getChargeIndicator ().isValue ();

            // Remember for next item
            if (eSurcharge.isUndefined ())
              eSurcharge = ETriState.valueOf (bItemIsSurcharge);
            final boolean bSwapSigns = bItemIsSurcharge != eSurcharge.isTrue ();

            final Ebi41ReductionAndSurchargeBaseType aEbiRSItem = new Ebi41ReductionAndSurchargeBaseType ();
            // Amount is mandatory
            final BigDecimal aAmount = aUBLAllowanceCharge.getAmountValue ();
            aEbiRSItem.setAmount (bSwapSigns ? aAmount.negate () : aAmount);

            // Base amount is optional
            if (aUBLAllowanceCharge.getBaseAmount () != null)
              aEbiRSItem.setBaseAmount (aUBLAllowanceCharge.getBaseAmountValue ());
            if (aEbiRSItem.getBaseAmount () == null)
              aEbiRSItem.setBaseAmount (aEbiBaseAmount);

            if (aUBLAllowanceCharge.getMultiplierFactorNumeric () != null)
            {
              // Percentage is optional
              final BigDecimal aPerc = aUBLAllowanceCharge.getMultiplierFactorNumericValue ()
                                                          .multiply (CGlobal.BIGDEC_100);
              aEbiRSItem.setPercentage (bSwapSigns ? aPerc.negate () : aPerc);
            }

            if (eSurcharge.isTrue ())
            {
              aEbiRSDetails.getReductionListLineItemOrSurchargeListLineItemOrOtherVATableTaxListLineItem ()
                           .add (new ObjectFactory ().createSurchargeListLineItem (aEbiRSItem));
              aEbiBaseAmount = aEbiBaseAmount.add (aEbiRSItem.getAmount ());
            }
            else
            {
              aEbiRSDetails.getReductionListLineItemOrSurchargeListLineItemOrOtherVATableTaxListLineItem ()
                           .add (new ObjectFactory ().createReductionListLineItem (aEbiRSItem));
              aEbiBaseAmount = aEbiBaseAmount.subtract (aEbiRSItem.getAmount ());
            }
          }
          aEbiListLineItem.setReductionAndSurchargeListLineItemDetails (aEbiRSDetails);
        }

        // Delivery per line item
        if (aUBLLine.getDeliveryCount () > 0)
        {
          // Delivery address
          final int nDeliveryIndex = 0;
          final DeliveryType aUBLDelivery = aUBLLine.getDeliveryAtIndex (0);

          if (aUBLDelivery.getActualDeliveryDate () != null)
          {
            final Ebi41DeliveryType aEbiDelivery = new Ebi41DeliveryType ();

            aEbiDelivery.setDate (aUBLDelivery.getActualDeliveryDateValue ());

            // Address present?
            if (aUBLDelivery.getDeliveryLocation () != null &&
                aUBLDelivery.getDeliveryLocation ().getAddress () != null)
            {
              final Ebi41AddressType aEbiAddress = new Ebi41AddressType ();
              EbInterface41Helper.setAddressData (aUBLDelivery.getDeliveryLocation ().getAddress (),
                                                  aEbiAddress,
                                                  "Delivery",
                                                  aTransformationErrorList,
                                                  m_aContentLocale,
                                                  m_aDisplayLocale);

              // Check delivery party
              String sAddressName = null;
              if (aUBLDelivery.getDeliveryParty () != null)
                for (final PartyNameType aUBLPartyName : aUBLDelivery.getDeliveryParty ().getPartyName ())
                {
                  sAddressName = StringHelper.trim (aUBLPartyName.getNameValue ());
                  if (StringHelper.hasText (sAddressName))
                    break;
                }

              // As fallback use accounting customer party
              if (StringHelper.hasNoText (sAddressName) &&
                  aUBLDoc.getAccountingCustomerParty () != null &&
                  aUBLDoc.getAccountingCustomerParty ().getParty () != null)
              {
                for (final PartyNameType aUBLPartyName : aUBLDoc.getAccountingCustomerParty ()
                                                                .getParty ()
                                                                .getPartyName ())
                {
                  sAddressName = StringHelper.trim (aUBLPartyName.getNameValue ());
                  if (StringHelper.hasText (sAddressName))
                    break;
                }
              }
              aEbiAddress.setName (sAddressName);

              if (StringHelper.hasNoText (aEbiAddress.getName ()))
                aTransformationErrorList.addError ("Delivery[" + nDeliveryIndex + "]/DeliveryParty",
                                                   EText.DELIVERY_WITHOUT_NAME.getDisplayText (m_aDisplayLocale));

              aEbiDelivery.setAddress (aEbiAddress);
            }

            aEbiListLineItem.setDelivery (aEbiDelivery);
          }
        }

        // Add the item to the list
        aEbiItemList.getListLineItem ().add (aEbiListLineItem);
        nLineIndex++;
      }
      aEbiDetails.getItemList ().add (aEbiItemList);
      aEbiDoc.setDetails (aEbiDetails);
    }

    if (aEbiVAT.hasNoVATItemEntries ())
    {
      aTransformationErrorList.addError ("CreditNoteLine", EText.VAT_ITEM_MISSING.getDisplayText (m_aDisplayLocale));
      if (false)
      {
        // No default in this case
        final Ebi41VATItemType aEbiVATItem = new Ebi41VATItemType ();
        aEbiVATItem.setTaxedAmount (aTotalZeroPercLineExtensionAmount);
        final Ebi41VATRateType aEbiVATVATRate = new Ebi41VATRateType ();
        aEbiVATVATRate.setValue (BigDecimal.ZERO);
        aEbiVATItem.setVATRate (aEbiVATVATRate);
        aEbiVATItem.setAmount (aTotalZeroPercLineExtensionAmount);
        aEbiVAT.getVATItem ().add (aEbiVATItem);
      }
    }

    // Global reduction and surcharge
    if (aUBLDoc.hasAllowanceChargeEntries ())
    {
      // Start with quantity*unitPrice for base amount
      BigDecimal aEbiBaseAmount = aUBLDoc.getLegalMonetaryTotal ().getLineExtensionAmountValue ();
      final Ebi41ReductionAndSurchargeDetailsType aEbiRS = new Ebi41ReductionAndSurchargeDetailsType ();

      int nAllowanceChargeIndex = 0;
      for (final AllowanceChargeType aUBLAllowanceCharge : aUBLDoc.getAllowanceCharge ())
      {
        final boolean bItemIsSurcharge = aUBLAllowanceCharge.getChargeIndicator ().isValue ();

        final Ebi41ReductionAndSurchargeType aEbiRSItem = new Ebi41ReductionAndSurchargeType ();
        // Amount is mandatory
        final BigDecimal aAmount = aUBLAllowanceCharge.getAmountValue ();
        aEbiRSItem.setAmount (aAmount);

        // Base amount is optional
        if (aUBLAllowanceCharge.getBaseAmount () != null)
          aEbiRSItem.setBaseAmount (aUBLAllowanceCharge.getBaseAmountValue ());
        if (aEbiRSItem.getBaseAmount () == null)
          aEbiRSItem.setBaseAmount (aEbiBaseAmount);

        if (aUBLAllowanceCharge.getMultiplierFactorNumeric () != null)
        {
          // Percentage is optional
          final BigDecimal aPerc = aUBLAllowanceCharge.getMultiplierFactorNumericValue ().multiply (CGlobal.BIGDEC_100);
          aEbiRSItem.setPercentage (aPerc);
        }

        Ebi41VATRateType aEbiVATRate = null;
        for (final TaxCategoryType aUBLTaxCategory : aUBLAllowanceCharge.getTaxCategory ())
          if (aUBLTaxCategory.getPercent () != null)
          {
            aEbiVATRate = new Ebi41VATRateType ();
            aEbiVATRate.setValue (aUBLTaxCategory.getPercentValue ());
            if (false)
              aEbiVATRate.setTaxCode (aUBLTaxCategory.getIDValue ());
            break;
          }
        if (aEbiVATRate == null)
        {
          aTransformationErrorList.addError ("CreditNote/AllowanceCharge[" + nAllowanceChargeIndex + "]",
                                             EText.ALLOWANCE_CHARGE_NO_TAXRATE.getDisplayText (m_aDisplayLocale));
          // No default in this case
          if (false)
          {
            aEbiVATRate = new Ebi41VATRateType ();
            aEbiVATRate.setValue (BigDecimal.ZERO);
            aEbiVATRate.setTaxCode (ETaxCode.NOT_TAXABLE.getID ());
          }
        }
        aEbiRSItem.setVATRate (aEbiVATRate);

        if (bItemIsSurcharge)
        {
          aEbiRS.getReductionOrSurchargeOrOtherVATableTax ().add (new ObjectFactory ().createSurcharge (aEbiRSItem));
          aEbiBaseAmount = aEbiBaseAmount.add (aEbiRSItem.getAmount ());
        }
        else
        {
          aEbiRS.getReductionOrSurchargeOrOtherVATableTax ().add (new ObjectFactory ().createReduction (aEbiRSItem));
          aEbiBaseAmount = aEbiBaseAmount.subtract (aEbiRSItem.getAmount ());
        }
        aEbiDoc.setReductionAndSurchargeDetails (aEbiRS);
        ++nAllowanceChargeIndex;
      }
    }

    // PrepaidAmount is not supported!
    final MonetaryTotalType aUBLMonetaryTotal = aUBLDoc.getLegalMonetaryTotal ();
    if (aUBLMonetaryTotal.getPrepaidAmount () != null &&
        !MathHelper.isEqualToZero (aUBLMonetaryTotal.getPrepaidAmountValue ()))
    {
      aTransformationErrorList.addError ("CreditNote/LegalMonetaryTotal/PrepaidAmount",
                                         EText.PREPAID_NOT_SUPPORTED.getDisplayText (m_aDisplayLocale));
    }

    // Total gross amount
    if (aUBLMonetaryTotal.getTaxInclusiveAmountValue () != null)
      aEbiDoc.setTotalGrossAmount (aUBLMonetaryTotal.getTaxInclusiveAmountValue ());
    else
      aEbiDoc.setTotalGrossAmount (aUBLMonetaryTotal.getPayableAmountValue ());

    // Payable amount
    aEbiDoc.setPayableAmount (aUBLMonetaryTotal.getPayableAmountValue ());

    // Always no payment
    final Ebi41PaymentMethodType aEbiPaymentMethod = new Ebi41PaymentMethodType ();
    final Ebi41NoPaymentType aEbiNoPayment = new Ebi41NoPaymentType ();
    aEbiPaymentMethod.setNoPayment (aEbiNoPayment);
    aEbiDoc.setPaymentMethod (aEbiPaymentMethod);

    final Ebi41PaymentConditionsType aEbiPaymentConditions = new Ebi41PaymentConditionsType ();
    if (aEbiPaymentConditions.getDueDate () == null)
    {
      // ebInterface requires due date
      if (aEbiPaymentConditions.hasDiscountEntries ())
        aTransformationErrorList.addError ("PaymentMeans/PaymentDueDate",
                                           EText.DISCOUNT_WITHOUT_DUEDATE.getDisplayTextWithArgs (m_aDisplayLocale));
    }
    else
    {
      // Independent if discounts are present or not
      aEbiDoc.setPaymentConditions (aEbiPaymentConditions);
    }

    // Delivery
    final Ebi41DeliveryType aEbiDelivery = new Ebi41DeliveryType ();
    {
      // Delivery address (since UBL 2.1)
      int nDeliveryIndex = 0;
      for (final DeliveryType aUBLDelivery : aUBLDoc.getDelivery ())
      {
        if (aUBLDelivery.getActualDeliveryDate () != null)
        {
          // Use the first delivery with a delivery date
          aEbiDelivery.setDate (aUBLDelivery.getActualDeliveryDateValue ());

          // Address
          if (aUBLDelivery.getDeliveryLocation () != null && aUBLDelivery.getDeliveryLocation ().getAddress () != null)
          {
            final Ebi41AddressType aEbiAddress = new Ebi41AddressType ();
            EbInterface41Helper.setAddressData (aUBLDelivery.getDeliveryLocation ().getAddress (),
                                                aEbiAddress,
                                                "Delivery",
                                                aTransformationErrorList,
                                                m_aContentLocale,
                                                m_aDisplayLocale);

            // Check delivery party
            String sAddressName = null;
            if (aUBLDelivery.getDeliveryParty () != null)
              for (final PartyNameType aUBLPartyName : aUBLDelivery.getDeliveryParty ().getPartyName ())
              {
                sAddressName = StringHelper.trim (aUBLPartyName.getNameValue ());
                if (StringHelper.hasText (sAddressName))
                  break;
              }

            // As fallback use accounting customer party
            if (StringHelper.hasNoText (sAddressName) &&
                aUBLDoc.getAccountingCustomerParty () != null &&
                aUBLDoc.getAccountingCustomerParty ().getParty () != null)
            {
              for (final PartyNameType aUBLPartyName : aUBLDoc.getAccountingCustomerParty ()
                                                              .getParty ()
                                                              .getPartyName ())
              {
                sAddressName = StringHelper.trim (aUBLPartyName.getNameValue ());
                if (StringHelper.hasText (sAddressName))
                  break;
              }
            }
            aEbiAddress.setName (sAddressName);

            if (StringHelper.hasNoText (aEbiAddress.getName ()))
              aTransformationErrorList.addError ("Delivery[" + nDeliveryIndex + "]/DeliveryParty",
                                                 EText.DELIVERY_WITHOUT_NAME.getDisplayText (m_aDisplayLocale));

            aEbiDelivery.setAddress (aEbiAddress);
          }
          break;
        }
        ++nDeliveryIndex;
      }

      if (aEbiDelivery.getDate () == null)
      {
        // No delivery date is present - check for service period
        final PeriodType aUBLCreditNotePeriod = ContainerHelper.getSafe (aUBLDoc.getInvoicePeriod (), 0);
        if (aUBLCreditNotePeriod != null)
        {
          final XMLGregorianCalendar aStartDate = aUBLCreditNotePeriod.getStartDateValue ();
          final XMLGregorianCalendar aEndDate = aUBLCreditNotePeriod.getEndDateValue ();
          if (aStartDate != null)
          {
            if (aEndDate == null)
            {
              // It's just a date
              aEbiDelivery.setDate (aStartDate);
            }
            else
            {
              // It's a period!
              final Ebi41PeriodType aEbiPeriod = new Ebi41PeriodType ();
              aEbiPeriod.setFromDate (aStartDate);
              aEbiPeriod.setToDate (aEndDate);
              aEbiDelivery.setPeriod (aEbiPeriod);
            }
          }
        }
      }
    }

    if (m_bStrictERBMode)
    {
      if (aEbiDelivery.getDate () == null && aEbiDelivery.getPeriod () == null)
        aTransformationErrorList.addError ("CreditNote", EText.ERB_NO_DELIVERY_DATE.getDisplayText (m_aDisplayLocale));
    }

    if (aEbiDelivery.getDate () != null || aEbiDelivery.getPeriod () != null)
      aEbiDoc.setDelivery (aEbiDelivery);

    return aEbiDoc;
  }
}