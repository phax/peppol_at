package at.gv.brz.transform.ubl2ebi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.phloc.commons.error.EErrorLevel;
import com.phloc.commons.io.IReadableResource;
import com.phloc.commons.io.file.FileUtils;
import com.phloc.commons.io.file.FilenameHelper;
import com.phloc.commons.io.file.filter.FilenameFilterEndsWith;
import com.phloc.commons.io.file.iterate.FileSystemRecursiveIterator;
import com.phloc.commons.io.resource.FileSystemResource;
import com.phloc.commons.xml.serialize.XMLWriter;
import com.phloc.ebinterface.EbInterface40Marshaller;
import com.phloc.ebinterface.v40.Ebi40InvoiceType;
import com.phloc.ubl.UBL20Reader;
import com.phloc.validation.error.ErrorList;

import eu.europa.ec.cipa.test.ETestFileType;
import eu.europa.ec.cipa.test.TestFiles;

/**
 * Test class for class {@link PEPPOLUBL20ToEbInterface40Converter}.
 * 
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class PEPPOLUBL20ToEbInterface40ConverterTest {
  private static final Logger s_aLogger = LoggerFactory.getLogger (PEPPOLUBL20ToEbInterface40ConverterTest.class);

  @Test
  public void testConvertPEPPOLInvoice () {
    final List <IReadableResource> aTestFiles = new ArrayList <IReadableResource> ();
    if (false)
      aTestFiles.addAll (TestFiles.getSuccessFiles (ETestFileType.INVOICE));
    for (final File aFile : FileSystemRecursiveIterator.create (new File ("src/test/resources/ubl20"),
                                                                new FilenameFilterEndsWith (".xml")))
      aTestFiles.add (new FileSystemResource (aFile));

    // For all PEPPOL test invoices
    for (final IReadableResource aRes : aTestFiles) {
      s_aLogger.info (aRes.getPath ());
      assertTrue (aRes.exists ());

      // Read UBL
      final InvoiceType aUBLInvoice = UBL20Reader.readInvoice (aRes);
      assertNotNull (aUBLInvoice);

      // Convert to ebInterface
      final ErrorList aLogger = new ErrorList ();
      final Ebi40InvoiceType aEbInvoice = PEPPOLUBL20ToEbInterface40Converter.convertToEbInterface (aUBLInvoice,
                                                                                                    aLogger,
                                                                                                    Locale.GERMANY);
      assertTrue (aRes.getPath () + ": " + aLogger.toString (),
                  aLogger.getMostSevereErrorLevel ().isLessSevereThan (EErrorLevel.ERROR));
      assertNotNull (aEbInvoice);

      if (aLogger.getMostSevereErrorLevel ().isMoreOrEqualSevereThan (EErrorLevel.WARN))
        s_aLogger.info ("  " + aLogger.getAllItems ());

      // Convert ebInterface to XML
      final Document aDocEb = new EbInterface40Marshaller ().write (aEbInvoice);
      assertNotNull (aDocEb);

      XMLWriter.writeToStream (aDocEb,
                               FileUtils.getOutputStream ("generated-ebi40-files/" +
                                                          FilenameHelper.getWithoutPath (aRes.getPath ())));
    }
  }
}
