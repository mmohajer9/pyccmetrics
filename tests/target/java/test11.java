
package java;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.*;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ViewForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.part.EditorActionBarContributor;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.junit.runner.ITestRunListener;
public class TestRunnerViewPart extends ViewPart implements ITestRunListener, IPropertyChangeListener {
	public static final String NAME= "org.eclipse.jdt.junit.ResultView";  	
	protected int fExecutedTests;
	protected int fErrors;
	protected int fFailures;
	private int fTestCount;
	private Map fTestInfos = new HashMap();
	private TestRunInfo fFirstFailure;
	private ProgressBar fProgressBar;
	private ProgressImages fProgressImages;
	private Image fViewImage;
	private CounterPanel fCounterPanel;
	private boolean fShowOnErrorOnly= false;
	private FailureTraceView fFailureView;
	private Vector fTestRunViews = new Vector();
	private ITestRunView fActiveRunView;
	private boolean fIsDisposed= false;
	private IType fTestType;
	private String fLaunchMode;
	private ILaunch fLastLaunch= null;
	private RemoteTestRunnerClient fTestRunnerClient;
	final Image fStackViewIcon= TestRunnerViewPart.createImage("cview16/stackframe.gif");	final Image fTestRunOKIcon= TestRunnerViewPart.createImage("cview16/junitsucc.gif"); 	final Image fTestRunFailIcon= TestRunnerViewPart.createImage("cview16/juniterr.gif"); 	final Image fTestRunOKDirtyIcon= TestRunnerViewPart.createImage("cview16/junitsuccq.gif"); 	final Image fTestRunFailDirtyIcon= TestRunnerViewPart.createImage("cview16/juniterrq.gif"); 	
	Image fOriginalViewImage= null;
	IElementChangedListener fDirtyListener= null;
	private class StopAction extends Action{
		public StopAction() {
			setText(JUnitMessages.getString("TestRunnerViewPart.stopaction.text"));			setToolTipText(JUnitMessages.getString("TestRunnerViewPart.stopaction.tooltip"));			setDisabledImageDescriptor(JUnitPlugin.getImageDescriptor("dlcl16/stop.gif")); 			setHoverImageDescriptor(JUnitPlugin.getImageDescriptor("clcl16/stop.gif")); 			setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/stop.gif")); 		}
		public void run() {
			stopTest();
		}
	}
	private class RerunAction extends Action{
		public RerunAction() {
			setText(JUnitMessages.getString("TestRunnerViewPart.rerunaction.label")); 			setToolTipText(JUnitMessages.getString("TestRunnerViewPart.rerunaction.tooltip")); 			setDisabledImageDescriptor(JUnitPlugin.getImageDescriptor("dlcl16/relaunch.gif")); 			setHoverImageDescriptor(JUnitPlugin.getImageDescriptor("clcl16/relaunch.gif")); 			setImageDescriptor(JUnitPlugin.getImageDescriptor("elcl16/relaunch.gif")); 		}
		public void run(){
			rerunTestRun();
		}
	}
	private class DirtyListener implements IElementChangedListener {
		public void elementChanged(ElementChangedEvent event) {
			processDelta(event.getDelta());				
		}
		private boolean processDelta(IJavaElementDelta delta) {
			int kind= delta.getKind();
			int details= delta.getFlags();
			int type= delta.getElement().getElementType();
			switch (type) {
								case IJavaElement.JAVA_MODEL:
				case IJavaElement.JAVA_PROJECT:
				case IJavaElement.PACKAGE_FRAGMENT_ROOT:
				case IJavaElement.PACKAGE_FRAGMENT:
										if (kind != IJavaElementDelta.CHANGED || details != IJavaElementDelta.F_CHILDREN) {
						codeHasChanged();
						return false;
					}
					break;
				case IJavaElement.COMPILATION_UNIT:
					ICompilationUnit unit= (ICompilationUnit)delta.getElement();
										if (unit.isWorkingCopy()) {
												return true;
					} else {
						codeHasChanged();
						return false;
					}
				case IJavaElement.CLASS_FILE:
										return true;
				default:
					codeHasChanged();
					return false;	
			}
			IJavaElementDelta[] affectedChildren= delta.getAffectedChildren();
			if (affectedChildren == null)
				return true;
			for (int i= 0; i < affectedChildren.length; i++) {
				if (!processDelta(affectedChildren[i]))
					return false;
			}
			return true;			
		}
	}
	public void stopTest() {
		if (fTestRunnerClient != null)
			fTestRunnerClient.stopTest();
	}
	public void rerunTestRun() {
		if (fLastLaunch != null && fLastLaunch.getLaunchConfiguration() != null) {
			try {
				fLastLaunch.getLaunchConfiguration().launch(fLastLaunch.getLaunchMode(), null);		
			} catch (CoreException e) {
				ErrorDialog.openError(getSite().getShell(), 
					JUnitMessages.getString("TestRunnerViewPart.error.cannotrerun"), e.getMessage(), e.getStatus() 				);
			}
		}
	}
	public void testRunStarted(final int testCount){
		reset(testCount);
		fShowOnErrorOnly= JUnitPreferencePage.getShowOnErrorOnly();
		fExecutedTests++;
	}
	public void testRunEnded(long elapsedTime){
		fExecutedTests--;
		String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.finish", elapsedTimeAsString(elapsedTime)); 		postInfo(msg);
		postAsyncRunnable(new Runnable() {				
			public void run() {
				if(isDisposed()) 
					return;	
				if (fFirstFailure != null) {
					fActiveRunView.setSelectedTest(fFirstFailure.fTestName);
					handleTestSelected(fFirstFailure.fTestName);
				}
				updateViewIcon();
				if (fDirtyListener == null) {
					fDirtyListener= new DirtyListener();
					JavaCore.addElementChangedListener(fDirtyListener);
				}
			}
		});	
	}
	private void updateViewIcon() {
		if (fErrors+fFailures > 0) 
			fViewImage= fTestRunFailIcon;
		else 
			fViewImage= fTestRunOKIcon;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);	
	}
	private String elapsedTimeAsString(long runTime) {
		return NumberFormat.getInstance().format((double)runTime/1000);
	}
	public void testRunStopped(final long elapsedTime) {
		String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.stopped", elapsedTimeAsString(elapsedTime)); 		postInfo(msg);
		postAsyncRunnable(new Runnable() {				
			public void run() {
				if(isDisposed()) 
					return;	
				resetViewIcon();
			}
		});	
	}
	private void resetViewIcon() {
		fViewImage= fOriginalViewImage;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}
	public void testRunTerminated() {
		String msg= JUnitMessages.getString("TestRunnerViewPart.message.terminated"); 		showMessage(msg);
	}
	private void showMessage(String msg) {
		showInformation(msg);
		postError(msg);
	}
	public void testStarted(String testName) {
				if (!fShowOnErrorOnly && fExecutedTests == 1) 
			postShowTestResultsView();
		postInfo(JUnitMessages.getFormattedString("TestRunnerViewPart.message.started", testName)); 		TestRunInfo testInfo= getTestInfo(testName);
		if (testInfo == null) 
			fTestInfos.put(testName, new TestRunInfo(testName));
	}
	public void testEnded(String testName){
		postEndTest(testName);
		fExecutedTests++;
	}
	public void testFailed(int status, String testName, String trace){
		TestRunInfo testInfo= getTestInfo(testName);
		if (testInfo == null) {
			testInfo= new TestRunInfo(testName);
			fTestInfos.put(testName, testInfo);
		}
		testInfo.fTrace= trace;
		testInfo.fStatus= status;
		if (status == ITestRunListener.STATUS_ERROR)
			fErrors++;
		else
			fFailures++;
		if (fFirstFailure == null)
			fFirstFailure= testInfo;
				if (fShowOnErrorOnly && (fErrors + fFailures == 1)) 
			postShowTestResultsView();
	}
	public void testReran(String className, String testName, int status, String trace) {
		if (status == ITestRunListener.STATUS_ERROR) {
			String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.error", new String[]{testName, className}); 			postError(msg); 
		} else if (status == ITestRunListener.STATUS_FAILURE) {
			String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.failure", new String[]{testName, className}); 			postError(msg);
		} else {
			String msg= JUnitMessages.getFormattedString("TestRunnerViewPart.message.success", new String[]{testName, className}); 			postInfo(msg);
		}
		String test= testName+"("+className+")"; 		TestRunInfo info= getTestInfo(test);
		updateTest(info, status);
		if (info.fTrace == null || !info.fTrace.equals(trace)) {
			info.fTrace= trace;
			showFailure(info.fTrace);
		}
	}
	private void updateTest(TestRunInfo info, final int status) {
		if (status == info.fStatus)
			return;
		if (info.fStatus == ITestRunListener.STATUS_OK) {
			if (status == ITestRunListener.STATUS_FAILURE) 
				fFailures++;
			else if (status == ITestRunListener.STATUS_ERROR)
				fErrors++;
		} else if (info.fStatus == ITestRunListener.STATUS_ERROR) {
			if (status == ITestRunListener.STATUS_OK) 
				fErrors--;
			else if (status == ITestRunListener.STATUS_FAILURE) {
				fErrors--;
				fFailures++;
			}
		} else if (info.fStatus == ITestRunListener.STATUS_FAILURE) {
			if (status == ITestRunListener.STATUS_OK) 
				fFailures--;
			else if (status == ITestRunListener.STATUS_ERROR) {
				fFailures--;
				fErrors++;
			}
		}			
		info.fStatus= status;	
		final TestRunInfo finalInfo= info;
		postAsyncRunnable(new Runnable() {
			public void run() {
				refreshCounters();
				for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
					ITestRunView v= (ITestRunView) e.nextElement();
					v.testStatusChanged(finalInfo);
				}
			}
		});
	}
	public void testTreeEntry(final String treeEntry){
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
					ITestRunView v= (ITestRunView) e.nextElement();
					v.newTreeEntry(treeEntry);
				}
			}
		});	
	}
	public void startTestRunListening(IType type, int port, ILaunch launch) {
		fTestType= type;
		fLaunchMode= launch.getLaunchMode();
		aboutToLaunch();
		if (fTestRunnerClient != null) {
			stopTest();
		}
		fTestRunnerClient= new RemoteTestRunnerClient();
		fTestRunnerClient.startListening(this, port);
		fLastLaunch= launch;
		String title= JUnitMessages.getFormattedString("TestRunnerViewPart.title", fTestType.getElementName()); 		setTitle(title);
		setTitleToolTip(fTestType.getFullyQualifiedName());
	}
	private void aboutToLaunch() {
		String msg= JUnitMessages.getString("TestRunnerViewPart.message.launching"); 		showInformation(msg);
		postInfo(msg);
		fViewImage= fOriginalViewImage;
		firePropertyChange(IWorkbenchPart.PROP_TITLE);
	}
	public void rerunTest(String className, String testName) {
		if (fTestRunnerClient != null && fTestRunnerClient.isRunning() && ILaunchManager.DEBUG_MODE.equals(fLaunchMode))
			fTestRunnerClient.rerunTest(className, testName);
		else {
			MessageDialog.openInformation(getSite().getShell(), 
				JUnitMessages.getString("TestRunnerViewPart.cannotrerun.title"),  				JUnitMessages.getString("TestRunnerViewPart.cannotrerurn.message")  			); 
		}
	}
	public synchronized void dispose(){
		fIsDisposed= true;
		stopTest();
		fProgressImages.dispose();
		JUnitPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
		fTestRunOKIcon.dispose();
		fTestRunFailIcon.dispose();
		fStackViewIcon.dispose();
		fTestRunOKDirtyIcon.dispose();
		fTestRunFailDirtyIcon.dispose();
	}
	private void start(final int total) {
		resetProgressBar(total);
		fCounterPanel.setTotal(total);
		fCounterPanel.setRunValue(0);	
	}
	private void resetProgressBar(final int total) {
		fProgressBar.setMinimum(0);
		fProgressBar.setSelection(0);
		fProgressBar.setForeground(getDisplay().getSystemColor(SWT.COLOR_GREEN));
		fProgressBar.setMaximum(total);
	}
	private void postSyncRunnable(Runnable r) {
		if (!isDisposed())
			getDisplay().syncExec(r);
	}
	private void postAsyncRunnable(Runnable r) {
		if (!isDisposed())
			getDisplay().asyncExec(r);
	}
	private void aboutToStart() {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed()) {
					for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
						ITestRunView v= (ITestRunView) e.nextElement();
						v.aboutToStart();
					}
				}
			}
		});
	}
	private void postEndTest(final String testName) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if(isDisposed()) 
					return;
				handleEndTest();
				for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
					ITestRunView v= (ITestRunView) e.nextElement();
					v.endTest(testName);
				}
			}
		});	
	}
	private void handleEndTest() {
		refreshCounters();
		updateProgressColor(fFailures+fErrors);
		fProgressBar.setSelection(fProgressBar.getSelection() + 1);
		if (fShowOnErrorOnly) {
			Image progress= fProgressImages.getImage(fExecutedTests, fTestCount, fErrors, fFailures);
			if (progress != fViewImage) {
				fViewImage= progress;
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		}
	}
	private void updateProgressColor(int failures) {
		if (failures > 0)
			fProgressBar.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
		else 
			fProgressBar.setForeground(getDisplay().getSystemColor(SWT.COLOR_GREEN));
	}
	private void refreshCounters() {
		fCounterPanel.setErrorValue(fErrors);
		fCounterPanel.setFailureValue(fFailures);
		fCounterPanel.setRunValue(fExecutedTests);
		updateProgressColor(fErrors + fFailures);
	}
	protected void postShowTestResultsView() {
		postAsyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				showTestResultsView();
			}
		});
	}
	public void showTestResultsView() {
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		TestRunnerViewPart testRunner= null;
		if (page != null) {
			try { 				testRunner= (TestRunnerViewPart)page.findView(TestRunnerViewPart.NAME);
				if(testRunner == null) {
					IWorkbenchPart activePart= page.getActivePart();
					testRunner= (TestRunnerViewPart)page.showView(TestRunnerViewPart.NAME);
										page.activate(activePart);
				} else {
					page.bringToTop(testRunner);
				}
			} catch (PartInitException pie) {
				JUnitPlugin.log(pie);
			}
		}
	}
	protected void postInfo(final String message) {
		postAsyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				getStatusLine().setErrorMessage(null);
				getStatusLine().setMessage(message);
			}
		});
	}
	protected void postError(final String message) {
		postAsyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				getStatusLine().setMessage(null);
				getStatusLine().setErrorMessage(message);
			}
		});
	}
	protected void showInformation(final String info){
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed())
					fFailureView.setInformation(info);
			}
		});
	}
	private CTabFolder createTestRunViews(Composite parent) {
		CTabFolder tabFolder= new CTabFolder(parent, SWT.TOP);
		tabFolder.setLayoutData(new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL));
		ITestRunView failureRunView= new FailureRunView(tabFolder, this); 
		ITestRunView testHierarchyRunView= new HierarchyRunView(tabFolder, this);
		fTestRunViews.addElement(failureRunView);
		fTestRunViews.addElement(testHierarchyRunView);
		tabFolder.setSelection(0);				
		fActiveRunView= (ITestRunView)fTestRunViews.firstElement();		
		tabFolder.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				testViewChanged(event);
			}
		});
		return tabFolder;
	}
	private void testViewChanged(SelectionEvent event) {
		for (Enumeration e= fTestRunViews.elements(); e.hasMoreElements();) {
			ITestRunView v= (ITestRunView) e.nextElement();
			if (((CTabFolder) event.widget).getSelection().getText() == v.getName()){
				v.setSelectedTest(fActiveRunView.getTestName());
				fActiveRunView= v;
				fActiveRunView.activate();
			}
		}
	}
	private SashForm createSashForm(Composite parent) {
		SashForm sashForm= new SashForm(parent, SWT.VERTICAL);		
		ViewForm top= new ViewForm(sashForm, SWT.NONE);
		CTabFolder tabFolder= createTestRunViews(top);
		tabFolder.setLayoutData(new TabFolderLayout());
		top.setContent(tabFolder);
		ViewForm bottom= new ViewForm(sashForm, SWT.NONE);
		ToolBar failureToolBar= new ToolBar(bottom, SWT.FLAT | SWT.WRAP);
		bottom.setTopCenter(failureToolBar);
		fFailureView= new FailureTraceView(bottom, this);
		bottom.setContent(fFailureView.getComposite()); 
		CLabel label= new CLabel(bottom, SWT.NONE);
		label.setText(JUnitMessages.getString("TestRunnerViewPart.label.failure")); 		label.setImage(fStackViewIcon);
		bottom.setTopLeft(label);
				ToolBarManager failureToolBarmanager= new ToolBarManager(failureToolBar);
		failureToolBarmanager.add(new EnableStackFilterAction(fFailureView));			
		failureToolBarmanager.update(true);
		sashForm.setWeights(new int[]{50, 50});
		return sashForm;
	}
	private void reset(final int testCount) {
		postAsyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed()) 
					return;
				fCounterPanel.reset();
				fFailureView.clear();
				clearStatus();
				start(testCount);
			}
		});
		fExecutedTests= 0;
		fFailures= 0;
		fErrors= 0;
		fTestCount= testCount;
		aboutToStart();
		fTestInfos.clear();
		fFirstFailure= null;
	}
	private void clearStatus() {
		getStatusLine().setMessage(null);
		getStatusLine().setErrorMessage(null);
	}
    public void setFocus() {
    	if (fActiveRunView != null)
    		fActiveRunView.setFocus();
    }
    public void createPartControl(Composite parent) {		
		GridLayout gridLayout= new GridLayout();
		gridLayout.marginWidth= 0;
		parent.setLayout(gridLayout);
		IActionBars actionBars= getViewSite().getActionBars();
		IToolBarManager toolBar= actionBars.getToolBarManager();
		toolBar.add(new StopAction());
		toolBar.add(new RerunAction());
		actionBars.updateActionBars();
		Composite counterPanel= createProgressCountPanel(parent);
		counterPanel.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		SashForm sashForm= createSashForm(parent);
		sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
		actionBars.setGlobalActionHandler(
			IWorkbenchActionConstants.COPY,
			new CopyTraceAction(fFailureView));
		JUnitPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
		fOriginalViewImage= getTitleImage();
		fProgressImages= new ProgressImages();
		WorkbenchHelp.setHelp(parent, IJUnitHelpContextIds.RESULTS_VIEW);
	}
	private IStatusLineManager getStatusLine() {
						IViewSite site= getViewSite();
		IWorkbenchPage page= site.getPage();
		IWorkbenchPart activePart= page.getActivePart();
		if (activePart instanceof IViewPart) {
			IViewPart activeViewPart= (IViewPart)activePart;
			IViewSite activeViewSite= activeViewPart.getViewSite();
			return activeViewSite.getActionBars().getStatusLineManager();
		}
		if (activePart instanceof IEditorPart) {
			IEditorPart activeEditorPart= (IEditorPart)activePart;
			IEditorActionBarContributor contributor= activeEditorPart.getEditorSite().getActionBarContributor();
			if (contributor instanceof EditorActionBarContributor) 
				return ((EditorActionBarContributor) contributor).getActionBars().getStatusLineManager();
		}
				return getViewSite().getActionBars().getStatusLineManager();
	}
	private Composite createProgressCountPanel(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		fProgressBar = new ProgressBar(composite, SWT.HORIZONTAL);
		fProgressBar.setLayoutData(
			new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		fCounterPanel = new CounterPanel(composite);
		fCounterPanel.setLayoutData(
			new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		return composite;
	}
	public TestRunInfo getTestInfo(String testName) {
		return (TestRunInfo) fTestInfos.get(testName);
	}
	public void handleTestSelected(String testName) {
		TestRunInfo testInfo= getTestInfo(testName);
		if (testInfo == null) {
			showFailure(""); 		} else {
			showFailure(testInfo.fTrace);
		}
	}
	private void showFailure(final String failure) {
		postSyncRunnable(new Runnable() {
			public void run() {
				if (!isDisposed())
					fFailureView.showFailure(failure);
			}
		});		
	}
	public IJavaProject getLaunchedProject() {
		return fTestType.getJavaProject();
	}
	protected static Image createImage(String path) {
		try {
			ImageDescriptor id= ImageDescriptor.createFromURL(JUnitPlugin.makeIconFileURL(path));
			return id.createImage();
		} catch (MalformedURLException e) {
					}  
		return null;
	}
	private boolean isDisposed() {
		return fIsDisposed || fCounterPanel.isDisposed();
	}
	private Display getDisplay() {
		return fCounterPanel.getDisplay();
	}
	public Image getTitleImage() {
		if (fOriginalViewImage == null)
			fOriginalViewImage= super.getTitleImage();
		if (fViewImage == null)
			return super.getTitleImage();
		return fViewImage;
	}
	public void propertyChange(PropertyChangeEvent event) {
		if (isDisposed())
			return;
		if (JUnitPreferencePage.SHOW_ON_ERROR_ONLY.equals(event.getProperty())) {
			if (!JUnitPreferencePage.getShowOnErrorOnly()) {
				fViewImage= fOriginalViewImage;
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		}
	}
	void codeHasChanged() {
		postAsyncRunnable(new Runnable() {
			public void run() {
				if (isDisposed())
					return;
				if (fDirtyListener != null) {
					JavaCore.removeElementChangedListener(fDirtyListener);
					fDirtyListener= null;
				}
				if (fViewImage == fTestRunOKIcon) 
					fViewImage= fTestRunOKDirtyIcon;
				else if (fViewImage == fTestRunFailIcon)
					fViewImage= fTestRunFailDirtyIcon;
				firePropertyChange(IWorkbenchPart.PROP_TITLE);
			}
		});
	}
}