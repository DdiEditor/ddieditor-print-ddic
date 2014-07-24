package dk.dda.ddieditor.print.ddic.command;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.ddialliance.ddieditor.model.DdiManager;
import org.ddialliance.ddieditor.model.lightxmlobject.LightXmlObjectType;
import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.persistenceaccess.PersistenceManager;
import org.ddialliance.ddieditor.util.DdiEditorConfig;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

import dk.dda.ddieditor.print.ddic.dialogs.ExportDDICDialog;
import dk.dda.ddieditor.print.ddic.util.PrintUtil;

public class ExportDDIC extends org.eclipse.core.commands.AbstractHandler {
	File ddiLFile = null;
	String lName = "DDI-L-";
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		// export input
		final ExportDDICDialog exportDDICDialog = new ExportDDICDialog(
				PlatformUI.getWorkbench().getDisplay().getActiveShell());
		int returnCode = exportDDICDialog.open();
		if (returnCode == Window.CANCEL) {
			return null;
		}

		// do export
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

								PersistenceManager.getInstance()
										.exportResoures(
												exportDDICDialog.ddiResource
														.getOrgName(),
												resources, ddiLFile);

								// transform ddi-l to ddi-c
								// get transformer
								Transformer transformer = new PrintUtil()
										.getDdiLToDdiDdiCTransformer();

								// do transformation
								transformer.transform(new StreamSource(ddiLFile
										.toURI().toURL().toString()),
										new StreamResult(new File(
										exportDDICDialog.path + File.separator
												+ exportDDICDialog.fileName)));

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
							.trans("OpenFileAction.mess.OpenFileError"), e.getMessage()); //$NON-NLS-1$
			MessageDialog.openError(PlatformUI.getWorkbench().getDisplay()
					.getActiveShell(), Translator.trans("ErrorTitle"), errMess);
		}
		return null;
	}

}
