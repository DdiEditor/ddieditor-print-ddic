package dk.dda.ddieditor.print.ddic.dialogs;


import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ddialliance.ddieditor.model.resource.DDIResourceType;
import org.ddialliance.ddieditor.persistenceaccess.PersistenceManager;
import org.ddialliance.ddieditor.ui.editor.Editor;
import org.ddialliance.ddieditor.ui.preference.PreferenceUtil;
import org.ddialliance.ddiftp.util.DDIFtpException;
import org.ddialliance.ddiftp.util.Translator;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

public class PrintDDICDialog extends Dialog {
	List<DDIResourceType> resources = new ArrayList<DDIResourceType>();
	public DDIResourceType ddiResource;
	public String path;
	public String fileName;
	
	Text fileNameText = null;

	public PrintDDICDialog(Shell parentShell) {
		super(parentShell);
	}
	
	private String getFileExtension(String f) {
	    String ext = "";
	    int i = f.lastIndexOf('.');
	    if (i > 0 &&  i < f.length() - 1) {
	      ext = f.substring(i + 1);
	    }
	    return ext;
	  }
	
	private String renameFileExtension
	  (String source, String newExtension)
	  {
	    String target;
	    String currentExtension = getFileExtension(source);

	    if (currentExtension.equals("")){
	      target = source + "." + newExtension;
	    }
	    else {
	      target = source.replaceFirst(Pattern.quote("." +
	          currentExtension) + "$", Matcher.quoteReplacement("." + newExtension));

	    }
	    return target;
	  }
	
	@Override
	protected Control createDialogArea(Composite parent) {
		// dialog setup
		Editor editor = new Editor();
		Group group = editor.createGroup(parent,
				Translator.trans("PrintDDICAction.properties"));
		this.getShell().setText(
				Translator.trans("PrintDDICAction.menu.PrintDDIC"));

		// selectable resources
		try {
			resources = PersistenceManager.getInstance().getResources();
		} catch (DDIFtpException e) {
			String errMess = MessageFormat
					.format(Translator
							.trans("PrintDDICAction.mess.PrintDDICError"), e.getMessage()); //$NON-NLS-1$
			MessageDialog.openError(PlatformUI.getWorkbench().getDisplay()
					.getActiveShell(), Translator.trans("ErrorTitle"), errMess);
		}

		// label
		editor.createLabel(group,
				Translator.trans("PrintDDICAction.resource.choose"));


		// resource combo
		String[] comboOptions = new String[resources.size()];
		for (int i = 0; i < comboOptions.length; i++) {
			comboOptions[i] = resources.get(i).getOrgName();
		}
		final Combo resouceCombo = editor.createCombo(group, comboOptions);

		// resource selection
		if (comboOptions.length == 1) {
			resouceCombo.select(0);
			ddiResource = resources.get(0);
		} else {
			resouceCombo.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int selection = ((Combo) e.getSource()).getSelectionIndex();
					ddiResource = resources.get(selection);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// do nothing
				}
			});
		}
		
		// PDF path
		editor.createLabel(group,
				Translator.trans("PrintDDICAction.filechooser.title"));
		final Text pathText = editor.createText(group, "", false);
		pathText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				path = ((Text) e.getSource()).getText();
			}
		});
		File lastBrowsedPath = PreferenceUtil.getLastBrowsedPath();
		if (lastBrowsedPath != null) {
			pathText.setText(lastBrowsedPath.getAbsolutePath());
			path = lastBrowsedPath.getAbsolutePath();
		}

		Button pathBrowse = editor.createButton(group,
				Translator.trans("PrintDDICAction.filechooser.browse"));
		pathBrowse.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dirChooser = new DirectoryDialog(PlatformUI
						.getWorkbench().getDisplay().getActiveShell());
				dirChooser.setText(Translator
						.trans("PrintDDICAction.filechooser.title"));
				PreferenceUtil.setPathFilter(dirChooser);
				path = dirChooser.open();
				if (path != null) {
					pathText.setText(path);
					PreferenceUtil.setLastBrowsedPath(path);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// do nothing
			}
		});

		// PDF file name
		fileNameText = editor.createTextInput(group,
				Translator.trans("PrintDDICAction.filename"), "", null);
		fileNameText.setData(true);
		if (comboOptions.length == 1) {
			fileNameText.setData(false);
			fileName = renameFileExtension(resouceCombo.getItem(0), "pdf");
			fileNameText.setText(fileName);
		}
		fileNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				Text text = ((Text) e.getSource());

				// do not change text on resource change selection
				if (!(Boolean) text.getData()) {
					text.setData(true);
					return;
				}
				fileName = text.getText();
			}
		});

		return null;
	}
}
