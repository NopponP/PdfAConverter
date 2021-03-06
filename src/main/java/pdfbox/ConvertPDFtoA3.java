package pdfbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.GregorianCalendar;

import org.apache.commons.io.FilenameUtils;
import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.jempbox.xmp.XMPSchema;
import org.apache.jempbox.xmp.XMPSchemaBasic;
import org.apache.jempbox.xmp.XMPSchemaPDF;
import org.apache.jempbox.xmp.pdfa.XMPSchemaPDFAId;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentCatalog;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDMarkInfo;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * The ConvertPDFtoPDFA3 method is used to convert any kind of PDF(.pdf) to
 * PDF/A-3
 * 
 * @author ETDA
 * 
 */
public class ConvertPDFtoA3 {

	private static float pdfVer = 1.7f;

	/**
	 * The convertPDFtoPDFA3(FilePaths, String, String, String) method is used
	 * to convert any kind of PDF(.pdf) to PDF/A-3
	 * 
	 * @param pdfa3Components
	 * @throws Exception
	 */
	public static void Convert(PDFA3Components pdfa3Components) throws Exception {
		File inputFile = new File(pdfa3Components.getInputFilePath());
		PDDocument doc = PDDocument.load(inputFile);
		File colorPFile = new File(pdfa3Components.getColorProfilePath());
		InputStream colorProfile = new FileInputStream(colorPFile);


		PDDocumentCatalog cat = makeA3compliant(doc, pdfa3Components);
		attachFile(doc, pdfa3Components.getEmbedFilePath());
		addOutputIntent(doc, cat, colorProfile);
		doc.setVersion(pdfVer);

		doc.save(pdfa3Components.getOutputFilePath());
		doc.close();
		File outputFile = new File(pdfa3Components.getOutputFilePath());
		if (outputFile.exists()) {
			System.out.println(pdfa3Components.getOutputFilePath());
		} else {
			System.out.println("Failed to convert.");
		}
	}

	private static void addOutputIntent(PDDocument doc, PDDocumentCatalog cat, InputStream colorProfile)
			throws IOException {
		if (cat.getOutputIntents().isEmpty()) {
			PDOutputIntent oi = new PDOutputIntent(doc, colorProfile);
			oi.setInfo("sRGB IEC61966-2.1");
			oi.setOutputCondition("sRGB IEC61966-2.1");
			oi.setOutputConditionIdentifier("sRGB IEC61966-2.1");
			oi.setRegistryName("http://www.color.org");
			cat.addOutputIntent(oi);
		}

	}

	private static void attachFile(PDDocument doc, String embedFilePath) throws IOException {
		PDEmbeddedFilesNameTreeNode efTree = new PDEmbeddedFilesNameTreeNode();

		File embedFile = new File(embedFilePath);
		String subType = FilenameUtils.getExtension(embedFilePath);
		String embedFileName = FilenameUtils.getName(embedFilePath);
		// first create the file specification, which holds the embedded file

		PDComplexFileSpecification fs = new PDComplexFileSpecification();
		fs.setFile(embedFileName);
		COSDictionary dict = fs.getCOSObject();
		// Relation "Source" for linking with eg. catalog
		dict.setName("AFRelationship", "Source");

		dict.setString("UF", embedFileName);

		InputStream is = new FileInputStream(embedFile);

		PDEmbeddedFile ef = new PDEmbeddedFile(doc, is);

		// set some of the attributes of the embedded file
		ef.setModDate(GregorianCalendar.getInstance());

		ef.setSize((int) embedFile.length());
		ef.setCreationDate(new GregorianCalendar());
		fs.setEmbeddedFile(ef);
		ef.setSubtype(subType);

		// now add the entry to the embedded file tree and set in the document.
		efTree.setNames(Collections.singletonMap(embedFileName, fs));

		// attachments are stored as part of the "names" dictionary in the
		PDDocumentCatalog catalog = doc.getDocumentCatalog();

		PDDocumentNameDictionary names = new PDDocumentNameDictionary(doc.getDocumentCatalog());
		names.setEmbeddedFiles(efTree);
		catalog.setNames(names);

		COSDictionary dict2 = catalog.getCOSObject();
		COSArray array = new COSArray();
		array.add(fs.getCOSObject());
		dict2.setItem("AF", array);
	}

	private static PDDocumentCatalog makeA3compliant(PDDocument doc, PDFA3Components pdfa3Components) throws Exception {
		PDDocumentCatalog cat = doc.getDocumentCatalog();
		PDDocumentInformation pdd = doc.getDocumentInformation();
		PDMetadata metadata = new PDMetadata(doc);
		cat.setMetadata(metadata);
		// jempbox version
		XMPMetadata xmp = new XMPMetadata();
		
		XMPSchemaPDFAId pdfaid = new XMPSchemaPDFAId(xmp);
		xmp.addSchema(pdfaid);

		XMPSchemaBasic xsb = xmp.addBasicSchema();
		xsb.setAbout("");

		// Set Application Name
		xsb.setCreatorTool("pdfbox");
		xsb.setCreateDate(GregorianCalendar.getInstance());

		PDDocumentInformation pdi = new PDDocumentInformation();
		pdi.setProducer(pdd.getProducer());
		pdi.setAuthor(pdd.getAuthor());
		pdi.setTitle(pdd.getTitle());
		pdi.setSubject(pdd.getSubject());
		pdi.setKeywords(pdd.getKeywords());

		// Set OID
		// pdi.setCustomMetadataValue("OID", "10.2.3.65.5");
		doc.setDocumentInformation(pdi);
		
		XMPSchema xsm = new XMPSchema(xmp, "rsm", "urn:etda:uncefact:data:standard:Invoice_CrossIndustryInvoice:2#");

		Element e = xsm.getElement();
		Element docFileNode = e.getOwnerDocument()
                .createElement("rsm:DocumentFileName");
		docFileNode.setTextContent(pdfa3Components.getDocumentFileName());
		e.appendChild(docFileNode);
		Element docTypeNode = e.getOwnerDocument()
                .createElement("rsm:DocumentType");
		docTypeNode.setTextContent(pdfa3Components.getDocumentType());
		e.appendChild(docTypeNode);
		Element docVerNode = e.getOwnerDocument()
                .createElement("rsm:Version");
		docVerNode.setTextContent(pdfa3Components.getDocumentVersion());
		e.appendChild(docVerNode);
		
		xmp.addSchema(xsm);
		
		XMPSchemaPDF pdf = xmp.addPDFSchema();
		pdf.setProducer(pdd.getProducer());
		pdf.setPDFVersion(String.valueOf(pdfVer));
		pdf.setAbout("");
		

		// Mandatory: PDF-A3 is tagged PDF which has to be expressed using a
		// MarkInfo dictionary (PDF A/3 Standard sec. 6.7.2.2)
		PDMarkInfo markinfo = new PDMarkInfo();
		markinfo.setMarked(true);
		doc.getDocumentCatalog().setMarkInfo(markinfo);

		pdfaid.setPart(3);
		pdfaid.setConformance(
				"U");/*
						 * All files are PDF/A-3, setConformance refers to the
						 * level conformance, e.g. PDF/A-3-B where B means only
						 * visually preservable, U means visually and unicode
						 * preservable and A -like in this case- means full
						 * compliance, i.e. visually, unicode and structurally
						 * preservable
						 */
		pdfaid.setAbout("");
		byte[] temp = xmp.asByteArray();

		metadata.importXMPMetadata(temp);
		return cat;
	}
}
