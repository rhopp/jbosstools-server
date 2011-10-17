/*******************************************************************************
 * Copyright (c) 2011 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.express.internal.ui.wizard;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.IValueChangeListener;
import org.eclipse.core.databinding.observable.value.ValueChangeEvent;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.IServerType;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.core.TaskModel;
import org.eclipse.wst.server.ui.internal.Messages;
import org.eclipse.wst.server.ui.internal.ServerUIPlugin;
import org.eclipse.wst.server.ui.internal.wizard.TaskWizard;
import org.eclipse.wst.server.ui.internal.wizard.WizardTaskUtil;
import org.eclipse.wst.server.ui.wizard.WizardFragment;
import org.jboss.ide.eclipse.as.core.util.IJBossToolingConstants;
import org.jboss.ide.eclipse.as.ui.UIUtil;
import org.jboss.tools.common.ui.databinding.DataBindingUtils;
import org.jboss.tools.common.ui.databinding.InvertingBooleanConverter;
import org.jboss.tools.openshift.express.client.ICartridge;
import org.jboss.tools.openshift.express.client.OpenshiftException;
import org.jboss.tools.openshift.express.internal.ui.OpenshiftUIActivator;

public class AdapterWizardPage extends AbstractOpenshiftWizardPage implements IWizardPage, PropertyChangeListener {

	private AdapterWizardPageModel model;
	private Combo suitableRuntimes;
	private IServerType serverTypeToCreate;
	private IRuntime runtimeDelegate;
	private Label domainLabel;
	private Label modeLabel;
	private Link addRuntimeLink;
	private Label runtimeLabel;
	private Button serverAdapterCheckbox;

	public AdapterWizardPage(ImportProjectWizard wizard, ImportProjectWizardModel model) {
		super(
				"Import Project",
				"Please select the destination for your local copy of the OpenShift Express repository, "
						+ "what branch to clone and setup your server adapter, ",
				"Server Adapter",
				wizard);
		this.model = new AdapterWizardPageModel(model);
		model.addPropertyChangeListener(this);
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		Group projectGroup = createProjectGroup(parent, dbc);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(projectGroup);

		Group serverAdapterGroup = createAdapterGroup(parent);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.FILL).grab(true, false).applyTo(serverAdapterGroup);
	}

	private Group createProjectGroup(Composite parent, DataBindingContext dbc) {
		Group projectGroup = new Group(parent, SWT.BORDER);
		projectGroup.setText("Git clone");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(projectGroup);
		GridLayoutFactory.fillDefaults().margins(6, 6).numColumns(4).applyTo(projectGroup);

		Label repoPathLabel = new Label(projectGroup, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(repoPathLabel);
		repoPathLabel.setText("Location");

		Button defaultRepoPathButton = new Button(projectGroup, SWT.CHECK);
		defaultRepoPathButton.setText("default");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(defaultRepoPathButton);
		defaultRepoPathButton.addSelectionListener(onDefaultRepoPath());
		IObservableValue defaultRepoButtonSelection = WidgetProperties.selection().observe(defaultRepoPathButton);

		Text repoPathText = new Text(projectGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false).applyTo(repoPathText);
		DataBindingUtils.bindMandatoryTextField(
				repoPathText, "Location", AdapterWizardPageModel.PROPERTY_REPO_PATH, model, dbc);
		dbc.bindValue(
				defaultRepoButtonSelection 
				, WidgetProperties.enabled().observe(repoPathText)
				, new UpdateValueStrategy().setConverter(new InvertingBooleanConverter())
				, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));

		Button browseRepoPathButton = new Button(projectGroup, SWT.PUSH);
		browseRepoPathButton.setText("Browse");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(browseRepoPathButton);
		browseRepoPathButton.addSelectionListener(onRepoPath());
		dbc.bindValue(
				defaultRepoButtonSelection
				, WidgetProperties.enabled().observe(browseRepoPathButton)
				, new UpdateValueStrategy().setConverter(new InvertingBooleanConverter())
				, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));

		defaultRepoButtonSelection.setValue(true);

		Label remoteNameLabel = new Label(projectGroup, SWT.NONE);
		remoteNameLabel.setText("Remote name");
		GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(remoteNameLabel);

		Button defaultRemoteNameButton = new Button(projectGroup, SWT.CHECK);
		defaultRemoteNameButton.setText("default");
		GridDataFactory.fillDefaults()
				.align(SWT.LEFT, SWT.CENTER).hint(100, SWT.DEFAULT).applyTo(defaultRemoteNameButton);
		defaultRemoteNameButton.addSelectionListener(onDefaultRemoteName());

		Text remoteNameText = new Text(projectGroup, SWT.BORDER);
		GridDataFactory.fillDefaults()
				.span(2, 1).align(SWT.LEFT, SWT.CENTER).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(remoteNameText);
		DataBindingUtils.bindMandatoryTextField(
				remoteNameText, "Remote name", AdapterWizardPageModel.PROPERTY_REMOTE_NAME, model, dbc);

		IObservableValue defaultRemoteNameSelection = WidgetProperties.selection().observe(defaultRemoteNameButton);
		dbc.bindValue(
				defaultRemoteNameSelection 
				, WidgetProperties.enabled().observe(remoteNameText)
				, new UpdateValueStrategy().setConverter(new InvertingBooleanConverter())
				, new UpdateValueStrategy(UpdateValueStrategy.POLICY_NEVER));
		defaultRemoteNameSelection.setValue(true);

		return projectGroup;
	}

	private SelectionListener onDefaultRepoPath() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				model.resetRepositoryPath();
			}
		};
	}

	private SelectionListener onRepoPath() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage("Choose the location to store your repository clone to...");
				String repositoryPath = dialog.open();
				if (repositoryPath != null) {
					model.setRepositoryPath(repositoryPath);
				}
			}
		};
	}

	private SelectionListener onDefaultRemoteName() {
		return new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				model.resetRemoteName();
			}
		};
	}

	private Group createAdapterGroup(Composite parent) {
		Group serverAdapterGroup = new Group(parent, SWT.BORDER);
		serverAdapterGroup.setText("JBoss Server adapter");
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginHeight = 6;
		fillLayout.marginWidth = 6;
		serverAdapterGroup.setLayout(fillLayout);
		fillServerAdapterGroup(serverAdapterGroup);

		return serverAdapterGroup;
	}

	protected void enableServerWidgets(boolean enabled) {
		suitableRuntimes.setEnabled(enabled);
		runtimeLabel.setEnabled(enabled);
		addRuntimeLink.setEnabled(enabled);
		domainLabel.setEnabled(enabled);
		modeLabel.setEnabled(enabled);
	}

	private void fillServerAdapterGroup(Group serverAdapterGroup) {
		Composite c = new Composite(serverAdapterGroup, SWT.NONE);
		c.setLayout(new FormLayout());
		serverAdapterCheckbox = new Button(c, SWT.CHECK);
		serverAdapterCheckbox.setText("Create a JBoss server adapter");
		final Button serverAdapterCheckbox2 = serverAdapterCheckbox;
		serverAdapterCheckbox.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				model.getParentModel().setProperty(AdapterWizardPageModel.CREATE_SERVER,
						serverAdapterCheckbox2.getSelection());
				enableServerWidgets(serverAdapterCheckbox2.getSelection());
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});

		runtimeLabel = new Label(c, SWT.NONE);
		runtimeLabel.setText("Local Runtime: ");

		suitableRuntimes = new Combo(c, SWT.READ_ONLY);
		addRuntimeLink = new Link(c, SWT.NONE);
		addRuntimeLink.setText("<a>" + Messages.addRuntime + "</a>");

		domainLabel = new Label(c, SWT.NONE);
		// appLabel = new Label(c, SWT.NONE);
		modeLabel = new Label(c, SWT.NONE);

		suitableRuntimes.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent e) {
				updateSelectedRuntimeDelegate();
			}

			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		addRuntimeLink.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IRuntimeType type = getValidRuntimeType();
				if (type != null)
					showRuntimeWizard(type);
			}
		});

		serverAdapterCheckbox.setLayoutData(UIUtil.createFormData2(0, 5, null, 0, 0, 5, null, 0));
		runtimeLabel.setLayoutData(UIUtil.createFormData2(serverAdapterCheckbox, 5, null, 0, 0, 5, null, 0));
		addRuntimeLink.setLayoutData(UIUtil.createFormData2(serverAdapterCheckbox, 5, null, 0, null, 0, 100, -5));
		suitableRuntimes.setLayoutData(UIUtil.createFormData2(serverAdapterCheckbox, 5, null, 0, runtimeLabel, 5,
				addRuntimeLink, -5));
		domainLabel.setLayoutData(UIUtil.createFormData2(suitableRuntimes, 5, null, 0, 0, 5, 100, 0));
		// appLabel.setLayoutData(UIUtil.createFormData2(domainLabel, 5, null,
		// 0, 0, 5, 100, 0));
		modeLabel.setLayoutData(UIUtil.createFormData2(domainLabel, 5, null, 0, 0, 5, 100, 0));
		serverAdapterCheckbox.setSelection(true);
		model.getParentModel().setProperty(AdapterWizardPageModel.CREATE_SERVER,
				serverAdapterCheckbox2.getSelection());
	}

	private void updateSelectedRuntimeDelegate() {
		if (suitableRuntimes.getSelectionIndex() != -1) {
			runtimeDelegate = ServerCore.findRuntime(suitableRuntimes.getItem(suitableRuntimes.getSelectionIndex()));
		} else {
			runtimeDelegate = null;
		}
		model.getParentModel().setProperty(AdapterWizardPageModel.RUNTIME_DELEGATE, runtimeDelegate);
	}

	private IRuntimeType getValidRuntimeType() {
		String cartridgeName = model.getParentModel().getApplicationCartridgeName();
		if (ICartridge.JBOSSAS_7.getName().equals(cartridgeName)) {
			return ServerCore.findRuntimeType(IJBossToolingConstants.AS_70);
		}
		return null;
	}

	private IServerType getServerTypeToCreate() {
		String cartridgeName = model.getParentModel().getApplicationCartridgeName();
		if (ICartridge.JBOSSAS_7.getName().equals(cartridgeName)) {
			return ServerCore.findServerType(IJBossToolingConstants.SERVER_AS_70);
		}
		return null;
	}

	private IRuntime[] getRuntimesOfType(String type) {
		ArrayList<IRuntime> validRuntimes = new ArrayList<IRuntime>();
		IRuntime[] allRuntimes = ServerCore.getRuntimes();
		for (int i = 0; i < allRuntimes.length; i++) {
			if (allRuntimes[i].getRuntimeType().getId().equals(type))
				validRuntimes.add(allRuntimes[i]);
		}
		return validRuntimes.toArray(new IRuntime[validRuntimes.size()]);
	}

	private void fillRuntimeCombo(Combo combo, IRuntime[] runtimes) {
		String[] names = new String[runtimes.length];
		for (int i = 0; i < runtimes.length; i++) {
			names[i] = runtimes[i].getName();
		}
		combo.setItems(names);
	}

	protected void onPageActivated(DataBindingContext dbc) {
		/*
		 * this needs to be initialized at page visibility since the default
		 * repository path default
		 */
		model.resetRepositoryPath();

		serverTypeToCreate = getServerTypeToCreate();
		model.getParentModel().setProperty(AdapterWizardPageModel.SERVER_TYPE, serverTypeToCreate);
		refreshValidRuntimes();
		if (suitableRuntimes.getItemCount() > 0) {
			suitableRuntimes.select(0);
			updateSelectedRuntimeDelegate();
		}
		IRuntimeType type = getValidRuntimeType();
		addRuntimeLink.setEnabled(type != null);

		try {
			domainLabel.setText("Host: " + model.getParentModel().getApplication().getApplicationUrl());
			modeLabel.setText("Mode: Source");
			model.getParentModel().setProperty(AdapterWizardPageModel.MODE, AdapterWizardPageModel.MODE_SOURCE);
		} catch (OpenshiftException ose) {
			OpenshiftUIActivator.getDefault().getLog()
					.log(new Status(IStatus.ERROR, OpenshiftUIActivator.PLUGIN_ID, ose.getMessage(), ose));
		}
	}

	protected void refreshValidRuntimes() {
		IRuntimeType type = getValidRuntimeType();
		if (type != null) {
			IRuntime[] runtimes = getRuntimesOfType(type.getId());
			fillRuntimeCombo(suitableRuntimes, runtimes);
		} else {
			suitableRuntimes.setItems(new String[0]);
		}
	}

	/* Stolen from NewManualServerComposite */
	protected int showRuntimeWizard(IRuntimeType runtimeType) {
		WizardFragment fragment = null;
		TaskModel taskModel = new TaskModel();
		final WizardFragment fragment2 = ServerUIPlugin.getWizardFragment(runtimeType.getId());
		if (fragment2 == null)
			return Window.CANCEL;

		try {
			IRuntimeWorkingCopy runtimeWorkingCopy = runtimeType.createRuntime(null, null);
			taskModel.putObject(TaskModel.TASK_RUNTIME, runtimeWorkingCopy);
		} catch (CoreException ce) {
			OpenshiftUIActivator.getDefault().getLog().log(ce.getStatus());
			return Window.CANCEL;
		}
		fragment = new WizardFragment() {
			protected void createChildFragments(List<WizardFragment> list) {
				list.add(fragment2);
				list.add(WizardTaskUtil.SaveRuntimeFragment);
			}
		};
		TaskWizard wizard2 = new TaskWizard(Messages.wizNewRuntimeWizardTitle, fragment, taskModel);
		wizard2.setForcePreviousAndNextButtons(true);
		WizardDialog dialog = new WizardDialog(getShell(), wizard2);
		int returnValue = dialog.open();
		refreshValidRuntimes();
		if (returnValue != Window.CANCEL) {
			IRuntime rt = (IRuntime) taskModel.getObject(TaskModel.TASK_RUNTIME);
			if (rt != null && rt.getName() != null && suitableRuntimes.indexOf(rt.getName()) != -1) {
				suitableRuntimes.select(suitableRuntimes.indexOf(rt.getName()));
			}
		}
		return returnValue;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		if (ImportProjectWizardModel.APPLICATION.equals(evt.getPropertyName())) {
			handleApplicationChanged();
		}
	}

	private void handleApplicationChanged() {
		// we need to enable or disable all the server widgets depending on
		// if we can make a server out of this
		serverTypeToCreate = getServerTypeToCreate();
		boolean canCreateServer = serverTypeToCreate != null;
		serverAdapterCheckbox.setEnabled(canCreateServer);
		serverAdapterCheckbox.setSelection(canCreateServer);
		enableServerWidgets(canCreateServer);
		refreshValidRuntimes();
		model.getParentModel().setProperty(AdapterWizardPageModel.SERVER_TYPE, serverTypeToCreate);
		model.getParentModel().setProperty(AdapterWizardPageModel.CREATE_SERVER, canCreateServer);
	}

}