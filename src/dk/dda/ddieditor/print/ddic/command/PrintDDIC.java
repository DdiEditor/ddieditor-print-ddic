package dk.dda.ddieditor.print.ddic.command;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.ddialliance.ddieditor.model.DdiManager;
import org.ddialliance.ddieditor.model.lightxmlobject.LightXmlObjectType;
import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.persistenceaccess.PersistenceManager;
import org.ddialliance.ddieditor.ui.dialogs.PrintDDICDialog;
import org.ddialliance.ddieditor.ui.util.PrintUtil;
import org.ddialliance.ddieditor.util.DdiEditorConfig;
import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

public class PrintDDIC extends AbstractHandler {
	File ddiLFile = null;
	File ddiCFile = null;
	File foFile = null;
	String lName = "DDI-L-";
	String cName = "DDI-C-";
	String foName = "FO-";

	private void convertFo2Pdf(File fo, File pdf) throws IOException,
			FOPException {
		// configure fopFactory as desired
		FopFactory fopFactory = FopFactory.newInstance();

		OutputStream out = null;

		try {
			FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
			// configure foUserAgent as desired

			// Setup output stream. Note: Using BufferedOutputStream
			// for performance reasons (helpful with FileOutputStreams).
			out = new FileOutputStream(pdf);
			out = new BufferedOutputStream(out);

			// Construct fop with desired output format
			Fop fop = fopFactory.newFop(MimeConstants.MIME_PDF, foUserAgent,
					out);

			// Setup JAXP using identity transformer
			// net.sf.saxon.TransformerFactoryImpl does not work!
			System.setProperty("javax.xml.transform.TransformerFactory",
					"org.apache.xalan.processor.TransformerFactoryImpl");
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(); // identity
																// transformer

			// Setup input stream
			Source src = new StreamSource(fo);

			// Resulting SAX events (the generated FO) must be piped through to
			// FOP
			Result res = new SAXResult(fop.getDefaultHandler());

			// Start XSLT transformation and FOP processing
			transformer.transform(src, res);

		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			out.close();
		}
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {

		// select DDI-C file to print
		final PrintDDICDialog printDDICDialog = new PrintDDICDialog(PlatformUI
				.getWorkbench().getDisplay().getActiveShell());
		int returnCode = printDDICDialog.open();
		if (returnCode == Window.CANCEL) {
			return null;
		}
		if (printDDICDialog.ddiResource == null) {
			MessageDialog.openError(PlatformUI.getWorkbench().getDisplay()
					.getActiveShell(), Translator
					.trans("PrintDDICAction.tooltip.PrintDDIC"), Translator
					.trans("PrintDDICAction.mess.ResourceNotSpecified"));
			return null;
		}

		try {
			PlatformUI.getWorkbench().getProgressService()
					.busyCursorWhile(new IRunnableWithProgress() {
						@Override
						public void run(IProgressMonitor monitor)
								throws InvocationTargetException,
								InterruptedException {

							// export the resource
							try {
								List<DDIResourceType> ddiResources = PersistenceManager
										.getInstance().getResources();
								List<String> resources = new ArrayList<String>();
								String workingResorce = PersistenceManager
										.getInstance().getWorkingResource();

								for (DDIResourceType ddiResource : ddiResources) {
									resources.add(ddiResource.getOrgName());
									PersistenceManager.getInstance()
											.setWorkingResource(
													ddiResource.getOrgName());
									for (LightXmlObjectType lightXmlObject : DdiManager
											.getInstance()
											.getStudyUnitsLight(null, null,
													null, null)
											.getLightXmlObjectList()
											.getLightXmlObjectList()) {
										lName = DdiEditorConfig
												.get(DdiEditorConfig.DDI_AGENCY_IDENTIFIER)
												+ "-" + lightXmlObject.getId();
									}
								}
								PersistenceManager.getInstance()
										.setWorkingResource(workingResorce);

								// create temp ddi-l file
								ddiLFile = File.createTempFile(lName, ".xml");
								ddiLFile.deleteOnExit();

								// create temp ddi-c file
								ddiCFile = File.createTempFile(cName, ".xml");
								ddiCFile.deleteOnExit();

								// create temp ddi-c file
								foFile = File.createTempFile(foName, ".fo");
								foFile.deleteOnExit();

								PersistenceManager.getInstance()
										.exportResoures(
												printDDICDialog.ddiResource
														.getOrgName(),
												resources, ddiLFile);

								// transform ddi-l to ddi-c
								// get transformer
								Transformer transformer = new PrintUtil()
										.getDdiLToDdiDdiCTransformer();

								// do transformation
								transformer.transform(new StreamSource(ddiLFile
										.toURI().toURL().toString()),
										new StreamResult(ddiCFile.toURI()
												.toURL().toString()));

								// transform from ddi-c to fo
								// get transformer
								transformer = new PrintUtil()
										.getDdiCToFoTransformer();

								// do transformation
								transformer.transform(new StreamSource(ddiCFile
										.toURI().toURL().toString()),
										new StreamResult(foFile.toURI().toURL()
												.toString()));

								// transform from fo to pdf
								convertFo2Pdf(foFile, new File(
										printDDICDialog.path + File.separator
												+ printDDICDialog.fileName));

							} catch (Exception e) {
								MessageDialog.openError(
										PlatformUI.getWorkbench().getDisplay()
												.getActiveShell(),
										Translator
												.trans("PrintDDICAction.mess.PrintDDICError"),
										e.getMessage());
							}
						}
					});
		} catch (Exception e) {
			String errMess = MessageFormat
					.format(Translator
							.trans("PrintDDICAction.mess.PrintDDICError"), e.getMessage()); //$NON-NLS-1$
			MessageDialog.openError(PlatformUI.getWorkbench().getDisplay()
					.getActiveShell(), Translator.trans("ErrorTitle"), errMess);
		}

		return null;
	}

}
