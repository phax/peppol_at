/**
 * Copyright (C) 2010 Bundesrechenzentrum GmbH
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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.CustomizationIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.ProfileIDType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_2.UBLVersionIDType;
import oasis.names.specification.ubl.schema.xsd.creditnote_2.CreditNoteType;
import at.gv.brz.transform.ubl2ebi.AbstractConverter;
import at.gv.brz.transform.ubl2ebi.CPeppolUBL;

import com.phloc.commons.string.StringHelper;
import com.phloc.validation.error.ErrorList;

import eu.europa.ec.cipa.peppol.identifier.doctype.IPeppolPredefinedDocumentTypeIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.IPeppolPredefinedProcessIdentifier;
import eu.europa.ec.cipa.peppol.identifier.process.PredefinedProcessIdentifierManager;

/**
 * Base class for PEPPOL 2.0 to ebInterface converter
 * 
 * @author philip
 */
@Immutable
public abstract class AbstractCreditNoteConverter extends AbstractConverter
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
  public AbstractCreditNoteConverter (@Nonnull final Locale aDisplayLocale,
                                      @Nonnull final Locale aContentLocale,
                                      final boolean bStrictERBMode)
  {
    super (aDisplayLocale, aContentLocale, bStrictERBMode);
  }

  /**
   * Check if the passed UBL invoice is transformable
   * 
   * @param aUBLCreditNote
   *        The UBL invoice to check
   */
  protected final void _checkConsistency (@Nonnull final CreditNoteType aUBLCreditNote,
                                          @Nonnull final ErrorList aTransformationErrorList)
  {
    // Check UBLVersionID
    final UBLVersionIDType aUBLVersionID = aUBLCreditNote.getUBLVersionID ();
    if (aUBLVersionID == null)
    {
      aTransformationErrorList.addError ("UBLVersionID",
                                         EText.NO_UBL_VERSION_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                         CPeppolUBL.UBL_VERSION));
    }
    else
    {
      final String sUBLVersionID = StringHelper.trim (aUBLVersionID.getValue ());
      if (!CPeppolUBL.UBL_VERSION.equals (sUBLVersionID))
      {
        aTransformationErrorList.addError ("UBLVersionID",
                                           EText.INVALID_UBL_VERSION_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                sUBLVersionID,
                                                                                                CPeppolUBL.UBL_VERSION));
      }
    }

    // Check ProfileID
    IPeppolPredefinedProcessIdentifier aProcID = null;
    final ProfileIDType aProfileID = aUBLCreditNote.getProfileID ();
    if (aProfileID == null)
    {
      aTransformationErrorList.addError ("ProfileID", EText.NO_PROFILE_ID.getDisplayText (m_aDisplayLocale));
    }
    else
    {
      final String sProfileID = StringHelper.trim (aProfileID.getValue ());
      aProcID = PredefinedProcessIdentifierManager.getProcessIdentifierOfID (sProfileID);
      if (aProcID == null)
      {
        aTransformationErrorList.addError ("ProfileID",
                                           EText.INVALID_PROFILE_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                            sProfileID));
      }
    }

    // Check CustomizationID
    // I'm not quite sure whether the document ID or "PEPPOL" should be used!
    if (false)
    {
      final CustomizationIDType aCustomizationID = aUBLCreditNote.getCustomizationID ();
      if (aCustomizationID == null)
        aTransformationErrorList.addError ("CustomizationID",
                                           EText.NO_CUSTOMIZATION_ID.getDisplayText (m_aDisplayLocale));
      else
        if (!CPeppolUBL.CUSTOMIZATION_SCHEMEID.equals (aCustomizationID.getSchemeID ()))
          aTransformationErrorList.addError ("CustomizationID/schemeID",
                                             EText.INVALID_CUSTOMIZATION_SCHEME_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                           aCustomizationID.getSchemeID (),
                                                                                                           CPeppolUBL.CUSTOMIZATION_SCHEMEID));
        else
          if (aProcID != null)
          {
            final String sCustomizationID = StringHelper.trim (aCustomizationID.getValue ());
            IPeppolPredefinedDocumentTypeIdentifier aMatchingDocID = null;
            for (final IPeppolPredefinedDocumentTypeIdentifier aDocID : aProcID.getDocumentTypeIdentifiers ())
              if (aDocID.getAsUBLCustomizationID ().equals (sCustomizationID))
              {
                // We found a match
                aMatchingDocID = aDocID;
                break;
              }
            if (aMatchingDocID == null)
              aTransformationErrorList.addError ("CustomizationID",
                                                 EText.INVALID_CUSTOMIZATION_ID.getDisplayTextWithArgs (m_aDisplayLocale,
                                                                                                        sCustomizationID));
          }
    }
  }
}
