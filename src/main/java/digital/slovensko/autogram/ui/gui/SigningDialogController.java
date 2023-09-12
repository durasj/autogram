package digital.slovensko.autogram.ui.gui;

import digital.slovensko.autogram.core.Autogram;
import digital.slovensko.autogram.core.SigningKey;
import digital.slovensko.autogram.core.visualization.Visualization;
import digital.slovensko.autogram.ui.Visualizer;
import digital.slovensko.autogram.util.DSSUtils;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.model.DSSDocument;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;

import static digital.slovensko.autogram.ui.gui.GUIValidationUtils.*;

public class SigningDialogController implements SuppressedFocusController, Visualizer {
    private final GUI gui;
    private final Autogram autogram;
    private SignaturesController signaturesController;
    private SignaturesNotValidatedDialogController signaturesNotValidatedDialogController;
    private SignaturesInvalidDialogController signaturesInvalidDialogController;
    private boolean signatureValidationCompleted = false;
    private final Visualization visualization;
    private Reports signatureValidationReports;
    private Reports signatureCheckReports;

    @FXML
    VBox mainBox;
    @FXML
    TextArea plainTextArea;
    @FXML
    WebView webView;
    @FXML
    VBox webViewContainer;
    @FXML
    ImageView imageVisualization;
    @FXML
    ScrollPane imageVisualizationContainer;
    @FXML
    public Button mainButton;
    @FXML
    public Button changeKeyButton;
    @FXML
    VBox unsupportedVisualizationInfoBox;
    @FXML
    VBox signaturesTable;
    @FXML
    Text signatureCheckMessage;

    public SigningDialogController(Visualization visualization, Autogram autogram, GUI gui) {
        this.visualization = visualization;
        this.gui = gui;
        this.autogram = autogram;
    }

    public void initialize() {
        refreshSigningKey();
        visualization.initialize(this);
        autogram.checkPDFACompliance(visualization.getJob());
    }

    public void onMainButtonPressed(ActionEvent event) {
        var signingKey = gui.getActiveSigningKey();
        if (signingKey == null) {
            autogram.pickSigningKeyAndThen(gui::setActiveSigningKey);
        } else {
            gui.disableSigning();
            getNodeForLoosingFocus().requestFocus();
            checkExistingSignatureValidityAndSign(signingKey);
        }
    }

    private void showSignaturesNotValidatedDialog() {
        if (signaturesNotValidatedDialogController == null)
            signaturesNotValidatedDialogController = new SignaturesNotValidatedDialogController(this);

        var root = GUIUtils.loadFXML(signaturesNotValidatedDialogController, "signatures-not-validated-dialog.fxml");
        var stage = new Stage();
        stage.setTitle("Upozornenie");
        stage.setScene(new Scene(root));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setOnCloseRequest(event -> {
            signaturesNotValidatedDialogController.onCancelAction();
        });
        GUIUtils.suppressDefaultFocus(stage, signaturesNotValidatedDialogController);
        stage.show();
    }

    private void showSignaturesInvalidDialog() {
        if (signaturesInvalidDialogController == null)
            signaturesInvalidDialogController = new SignaturesInvalidDialogController(this);

        var root = GUIUtils.loadFXML(signaturesInvalidDialogController, "signatures-invalid-dialog.fxml");
        var stage = new Stage();
        stage.setTitle("Upozornenie");
        stage.setScene(new Scene(root));
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setOnCloseRequest(event -> {
            signaturesInvalidDialogController.onCancelAction();
        });
        signaturesInvalidDialogController.initialize(signatureValidationReports);
        GUIUtils.suppressDefaultFocus(stage, signaturesInvalidDialogController);
        stage.show();

        return;
    }

    private void checkExistingSignatureValidityAndSign(SigningKey signingKey) {
        if (!signatureValidationCompleted) {
            showSignaturesNotValidatedDialog();
            return;
        }

        for (var signatureId : signatureValidationReports.getSimpleReport().getSignatureIdList()) {
            if (!signatureValidationReports.getSimpleReport().isValid(signatureId)) {
                showSignaturesInvalidDialog();
                return;
            }
        }

        sign();
    }

    public void sign() {
        var signingKey = gui.getActiveSigningKey();
        if (signingKey == null) {
            autogram.pickSigningKeyAndThen(gui::setActiveSigningKey);
        } else {
            autogram.sign(visualization.getJob(), signingKey);
        }
    }

    public void onChangeKeyButtonPressed(ActionEvent event) {
        gui.resetSigningKey();
        autogram.pickSigningKeyAndThen(gui::setActiveSigningKey);
    }

    public void onShowSignaturesButtonPressed(ActionEvent event) {
        if (signaturesController == null)
            signaturesController = new SignaturesController(signatureCheckReports, gui);

        var root = GUIUtils.loadFXML(signaturesController, "present-signatures-dialog.fxml");

        var stage = new Stage();
        stage.setTitle("Prítomné podpisy"); // TODO lepsi nazov
        stage.setScene(new Scene(root));
        GUIUtils.suppressDefaultFocus(stage, signaturesController);
        signaturesController.showSignatures();
        stage.show();

        if (signatureValidationCompleted)
            signaturesController.onSignatureValidationCompleted(signatureValidationReports);
    }

    public void onSignatureCheckCompleted(Reports reports) {
        signatureCheckReports = reports;
        renderSignatures(reports, false);
    }

    public void onSignatureValidationCompleted(Reports reports) {
        signatureValidationCompleted = true;
        signatureValidationReports = reports;
        renderSignatures(reports, true);
        if (signaturesController != null)
            signaturesController.onSignatureValidationCompleted(reports);

        if (signaturesNotValidatedDialogController != null)
            signaturesNotValidatedDialogController.onCancelAction();
    }

    public void renderSignatures(Reports reports, boolean isValidated) {
        if (reports == null) {
            signatureCheckMessage.setText("Dokument ešte neobsahuje žiadne podpisy.");
            return;
        }

        signaturesTable.getChildren().clear();
        signaturesTable.getChildren().addAll(
                createSignatureTableHeader(isValidated),
                createSignatureTableRows(reports, isValidated, e -> {
                    onShowSignaturesButtonPressed(null);
                }));

        var stage = (Stage) mainButton.getScene().getWindow();
        stage.sizeToScene();
    }

    public void refreshSigningKey() {
        mainButton.setDisable(false);
        SigningKey key = gui.getActiveSigningKey();
        if (key == null) {
            mainButton.setText("Vybrať podpisový certifikát");
            changeKeyButton.setVisible(false);
        } else {
            mainButton.setText("Podpísať ako " + DSSUtils.parseCN(key.getCertificate().getSubject().getRFC2253()));
            changeKeyButton.setVisible(true);
        }
    }

    public void close() {
        var window = mainButton.getScene().getRoot().getScene().getWindow();
        if (window instanceof Stage) {
            ((Stage) window).close();
        }
    }

    public void disableKeyPicking() {
        mainButton.setText("Načítavam certifikáty...");
        mainButton.setDisable(true);
    }

    public void disableSigning() {
        mainButton.setText("Prebieha podpisovanie...");
        mainButton.setDisable(true);
    }

    public void showPlainTextVisualization(String text) {
        plainTextArea.addEventFilter(ContextMenuEvent.CONTEXT_MENU_REQUESTED, Event::consume);
        plainTextArea.setText(text);
        plainTextArea.setVisible(true);
        plainTextArea.setManaged(true);
    }

    public void showHTMLVisualization(String html) {
        webView.setContextMenuEnabled(false);
        webView.getEngine().setJavaScriptEnabled(false);
        var engine = webView.getEngine();
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                engine.getDocument().getElementById("frame").setAttribute("srcdoc", html);
            }
        });
        engine.load(getClass().getResource("visualization-html.html").toExternalForm());
        webViewContainer.getStyleClass().add("autogram-visualizer-html");
        webViewContainer.setVisible(true);
        webViewContainer.setManaged(true);
    }

    public void showPDFVisualization(String base64EncodedPdf) {
        var engine = webView.getEngine();
        engine.setJavaScriptEnabled(true);
        engine.getLoadWorker().stateProperty().addListener((observable, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                engine.executeScript(
                        "displayPdf('" + base64EncodedPdf + "')");
            }
        });
        engine.load(getClass().getResource("visualization-pdf.html").toExternalForm());
        webViewContainer.getStyleClass().add("autogram-visualizer-pdf");
        webViewContainer.setVisible(true);
        webViewContainer.setManaged(true);
    }

    public void showImageVisualization(DSSDocument doc) {
        // TODO what about visualization
        imageVisualization.fitWidthProperty().bind(imageVisualizationContainer.widthProperty().subtract(4));
        imageVisualization.setImage(new Image(doc.openStream()));
        imageVisualization.setPreserveRatio(true);
        imageVisualization.setSmooth(true);
        imageVisualization.setCursor(Cursor.OPEN_HAND);
        imageVisualizationContainer.setPannable(true);
        imageVisualizationContainer.setFitToWidth(true);
        imageVisualizationContainer.setVisible(true);
        imageVisualizationContainer.setManaged(true);
    }

    public void showUnsupportedVisualization() {
        unsupportedVisualizationInfoBox.setVisible(true);
        unsupportedVisualizationInfoBox.setManaged(true);
    }

    @Override
    public Node getNodeForLoosingFocus() {
        return mainBox;
    }

    @Override
    public void setPrefWidth(double prefWidth) {
        mainBox.setPrefWidth(prefWidth);
    }

    public void enableSigning() {
        gui.refreshKeyOnAllJobs();
    }
}
