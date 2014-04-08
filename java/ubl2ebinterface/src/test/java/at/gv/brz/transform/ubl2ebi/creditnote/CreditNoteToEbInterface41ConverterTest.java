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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.xml.bind.Marshaller;

import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import at.gv.brz.transform.ubl2ebi.EbiNamespacePrefixMapper;

import com.phloc.commons.error.EErrorLevel;
import com.phloc.commons.io.IReadableResource;
import com.phloc.commons.io.file.FileUtils;
import com.phloc.commons.io.file.FilenameHelper;
import com.phloc.commons.io.file.filter.FilenameFilterEndsWith;
import com.phloc.commons.io.file.iterate.FileSystemIterator;
import com.phloc.commons.io.file.iterate.FileSystemRecursiveIterator;
import com.phloc.commons.io.resource.FileSystemResource;
import com.phloc.commons.jaxb.JAXBMarshallerUtils;
import com.phloc.commons.xml.serialize.XMLWriter;
import com.phloc.ebinterface.EbInterface41Marshaller;
import com.phloc.ebinterface.v41.Ebi41InvoiceType;
import com.phloc.ubl.UBL21Reader;
import com.phloc.validation.error.ErrorList;

import eu.europa.ec.cipa.test.ETestFileType;
import eu.europa.ec.cipa.test.TestFiles;

/**
 * Test class for class {@link CreditNoteToEbInterface41Converter}.
 * 
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class CreditNoteToEbInterface41ConverterTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (CreditNoteToEbInterface41ConverterTest.class);

  @Test
  public void testConvertPEPPOLCreditNoteLax ()
  {
    final List <IReadableResource> aTestFiles = new ArrayList <IReadableResource> ();
    if (false)
      aTestFiles.addAll (TestFiles.getSuccessFiles (ETestFileType.CREDITNOTE));
    for (final File aFile : FileSystemRecursiveIterator.create (new File ("src/test/resources/ubl20/creditnote"),
                                                                new FilenameFilterEndsWith (".xml")))
      aTestFiles.add (new FileSystemResource (aFile));

    // For all PEPPOL test invoices
    for (final IReadableResource aRes : aTestFiles)
    {
      s_aLogger.info (aRes.getPath ());
      assertTrue (aRes.exists ());

      // Read UBL
      final CreditNoteType aUBLCreditNote = UBL21Reader.readCreditNote (aRes);
      assertNotNull (aUBLCreditNote);

      // Convert to ebInterface
      final ErrorList aErrorList = new ErrorList ();
      final Ebi41InvoiceType aEbInvoice = new CreditNoteToEbInterface41Converter (Locale.GERMANY, Locale.GERMANY, false).convertToEbInterface (aUBLCreditNote,
                                                                                                                                               aErrorList);
      assertTrue (aRes.getPath () + ": " + aErrorList.toString (), aErrorList.isEmpty () ||
                                                                   aErrorList.getMostSevereErrorLevel ()
                                                                             .isLessSevereThan (EErrorLevel.ERROR));
      assertNotNull (aEbInvoice);

      if (!aErrorList.isEmpty () && aErrorList.getMostSevereErrorLevel ().isMoreOrEqualSevereThan (EErrorLevel.WARN))
        s_aLogger.info ("  " + aErrorList.getAllItems ());

      // Convert ebInterface to XML
      final Document aDocEb = new EbInterface41Marshaller ()
      {
        @Override
        protected void customizeMarshaller (@Nonnull final Marshaller aMarshaller)
        {
          JAXBMarshallerUtils.setSunNamespacePrefixMapper (aMarshaller, new EbiNamespacePrefixMapper ());
        }
      }.write (aEbInvoice);
      assertNotNull (aDocEb);

      XMLWriter.writeToStream (aDocEb,
                               FileUtils.getOutputStream ("generated-ebi41-files/" +
                                                          FilenameHelper.getWithoutPath (aRes.getPath ())));
    }
  }

  @Test
  public void testConvertPEPPOLInvoiceERB ()
  {
    final List <IReadableResource> aTestFiles = new ArrayList <IReadableResource> ();
    for (final File aFile : FileSystemIterator.create (new File ("src/test/resources/ubl20/creditnote"),
                                                       new FilenameFilterEndsWith (".xml")))
      aTestFiles.add (new FileSystemResource (aFile));

    // For all PEPPOL test invoices
    for (final IReadableResource aRes : aTestFiles)
    {
      s_aLogger.info (aRes.getPath ());
      assertTrue (aRes.exists ());

      // Read UBL
      final CreditNoteType aUBLCreditNote = UBL21Reader.readCreditNote (aRes);
      assertNotNull (aUBLCreditNote);

      // Convert to ebInterface
      final ErrorList aErrorList = new ErrorList ();
      final Ebi41InvoiceType aEbInvoice = new CreditNoteToEbInterface41Converter (Locale.GERMANY, Locale.GERMANY, true).convertToEbInterface (aUBLCreditNote,
                                                                                                                                              aErrorList);
      assertTrue (aRes.getPath () + ": " + aErrorList.toString (), aErrorList.getMostSevereErrorLevel ()
                                                                             .isLessSevereThan (EErrorLevel.ERROR));
      assertNotNull (aEbInvoice);

      if (aErrorList.getMostSevereErrorLevel ().isMoreOrEqualSevereThan (EErrorLevel.WARN))
        s_aLogger.info ("  " + aErrorList.getAllItems ());

      // Convert ebInterface to XML
      final Document aDocEb = new EbInterface41Marshaller ()
      {
        @Override
        protected void customizeMarshaller (@Nonnull final Marshaller aMarshaller)
        {
          JAXBMarshallerUtils.setSunNamespacePrefixMapper (aMarshaller, new EbiNamespacePrefixMapper ());
        }
      }.write (aEbInvoice);
      assertNotNull (aDocEb);

      XMLWriter.writeToStream (aDocEb,
                               FileUtils.getOutputStream ("generated-ebi41-files/" +
                                                          FilenameHelper.getWithoutPath (aRes.getPath ())));
    }
  }
}