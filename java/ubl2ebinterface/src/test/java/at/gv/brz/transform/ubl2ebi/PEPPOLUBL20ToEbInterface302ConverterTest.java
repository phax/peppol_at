package at.gv.brz.transform.ubl2ebi;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import oasis.names.specification.ubl.schema.xsd.invoice_2.InvoiceType;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import at.peppol.test.ETestFileType;
import at.peppol.test.TestFiles;

import com.phloc.commons.io.IReadableResource;
import com.phloc.commons.io.file.FileUtils;
import com.phloc.commons.io.file.filter.FilenameFilterEndsWith;
import com.phloc.commons.io.resource.FileSystemResource;
import com.phloc.commons.xml.serialize.XMLReader;
import com.phloc.ebinterface.EbInterface302Marshaller;
import com.phloc.ubl.UBL20Reader;

/**
 * Test class for class {@link PEPPOLUBL20ToEbInterface302Converter}.
 * 
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public class PEPPOLUBL20ToEbInterface302ConverterTest
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (PEPPOLUBL20ToEbInterface302ConverterTest.class);

  @Test
  public void testConvertPEPPOLInvoice () throws SAXException
  {
    final List <IReadableResource> aTestFiles = TestFiles.getSuccessFiles (ETestFileType.INVOICE);
    for (final File aFile : FileUtils.getDirectoryContent (new File ("src/test/resources/ubl20"),
                                                           new FilenameFilterEndsWith (".xml")))
      aTestFiles.add (new FileSystemResource (aFile));

    // For all PEPPOL test invoices
    for (final IReadableResource aRes : aTestFiles)
    {
      s_aLogger.info (aRes.getPath ());
      assertTrue (aRes.exists ());

      // Read XML
      final Document aDoc = XMLReader.readXMLDOM (aRes);
      assertNotNull (aDoc);

      // Read UBL
      final InvoiceType aUBLInvoice = UBL20Reader.readInvoice (aDoc);
      assertNotNull (aUBLInvoice);

      // Convert to ebInterface
      final com.phloc.ebinterface.v302.InvoiceType aEbInvoice = PEPPOLUBL20ToEbInterface302Converter.convertToEbInterface (aUBLInvoice);
      assertNotNull (aEbInvoice);

      // Convert ebInterface to XML
      final Document aDocEb = new EbInterface302Marshaller ().write (aEbInvoice);
      assertNotNull (aDocEb);
    }
  }
}
