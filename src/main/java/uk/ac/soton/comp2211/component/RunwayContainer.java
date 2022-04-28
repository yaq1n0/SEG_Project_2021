package uk.ac.soton.comp2211.component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import uk.ac.soton.comp2211.event.DeleteTarmacListener;
import uk.ac.soton.comp2211.event.WarningListener;
import uk.ac.soton.comp2211.exceptions.PositionException;
import uk.ac.soton.comp2211.exceptions.RunwayException;
import uk.ac.soton.comp2211.model.*;

/**
 * Component to contain data on each runway for an airport.
 */
public class RunwayContainer extends VBox {

    private static final Logger logger = LogManager.getLogger(RunwayContainer.class);
    
    private final RunwayView runwayView;
    private final ParameterBox parameterBox;
    private final ObstacleBox obstacleBox;
    private final BooleanProperty topView = new SimpleBooleanProperty();
    private final BooleanProperty colour = new SimpleBooleanProperty();
    private DeleteTarmacListener deleteTarmacListener;
    private WarningListener deletionWarningListener;
    private final Runway runway;
    
    public RunwayContainer(Runway runway, Stage stage) {
        super();
        
        this.runway = runway;
        this.topView.addListener((obs, oldVal, newVal) -> this.updateVisual());
        this.colour.addListener((obs, oldVal, newVal) -> this.updateColour());
        
        this.runwayView = new RunwayView(750, 300, this.runway);
        this.runwayView.updateTopDown();
        
        VBox viewBox = new VBox();
        HBox viewTools = new HBox();
        
        Button zoomInButton = new Button("+");
        Button zoomOutButton = new Button("-");
        // Pass the boolean property to the runway view
        zoomInButton.setOnAction((ActionEvent event) -> this.runwayView.zoomIn(topView));
        zoomOutButton.setOnAction((ActionEvent event) -> this.runwayView.zoomOut(topView));
        
        Button leftPan = new Button("<");
        Button rightPan = new Button(">");
        Button upPan = new Button("^");
        Button downPan = new Button ("v");
        leftPan.setOnAction((ActionEvent event) -> this.runwayView.move(10, 0, topView));
        rightPan.setOnAction((ActionEvent event) -> this.runwayView.move(-10, 0, topView));
        upPan.setOnAction((ActionEvent event) -> this.runwayView.move(0, 10, topView));
        downPan.setOnAction((ActionEvent event) -> this.runwayView.move(0, -10, topView));

        Button rotateClockWise = new Button("Rotate Clockwise");
        rotateClockWise.setOnAction((ActionEvent event) -> this.runwayView.drawRotated(5, topView));
        Button rotateAnti = new Button("Rotate Anti-Clockwise");
        rotateAnti.setOnAction((ActionEvent event) -> this.runwayView.drawRotated(-5, topView));
        
        viewTools.getChildren().addAll(leftPan, upPan, downPan, rightPan, zoomInButton, zoomOutButton, rotateClockWise, rotateAnti);
        viewTools.setAlignment(Pos.CENTER_RIGHT);
        viewBox.getChildren().addAll(this.runwayView, viewTools);
        
        this.parameterBox = new ParameterBox(runway.getOriginalValues());
        this.parameterBox.setPadding(new Insets(0, 10, 10, 10));
        
        // get obstacles needs to change for multiple obstacles
        Obstacle obs = runway.getTarmac().getObstacle();
        this.obstacleBox = new ObstacleBox(obs, this.runway.getLength());
        this.obstacleBox.setVerifyObstacleListener(this::enableRecalculation);
        this.obstacleBox.setInsertObstacleListener(() -> {
            final Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initOwner(stage);
            
            ObstacleSelect obstacleSelect = new ObstacleSelect(dialog, SystemModel.getObstacles());
            obstacleSelect.setPassObstacleListener(this::setObstacle);

            Scene dialogScene = new Scene(obstacleSelect, 300, 200);
            dialog.setScene(dialogScene);
            dialog.show();
        });
        this.obstacleBox.setObstacleClearListener(this::clearObstacle);

        HBox.setHgrow(this.parameterBox, Priority.ALWAYS);
        this.parameterBox.setStyle("-fx-border-color: black");
        HBox.setHgrow(obstacleBox, Priority.ALWAYS);
        this.obstacleBox.setStyle("-fx-border-color: black");


        this.obstacleBox.setPadding(new Insets(10, 10, 10, 10));

        // Delete button for runway
        Button delete = new Button("Delete Tarmac");
        delete.setOnAction(this::showDeletionWarning);
        
        Label designator = new Label(this.runway.getRunwayDesignator());
        
        HBox topBox = new HBox();
        topBox.getChildren().addAll(designator, delete);
        
        this.getChildren().addAll(topBox, viewBox, this.parameterBox, this.obstacleBox);
        this.setStyle("-fx-border-color: black;");
        this.setPadding(new Insets(10, 10, 10, 10));

        this.disableRecalculation();
    }

    /**
     * Open a dialog through the deletion warning listener.
     * @param event event
     */
    private void showDeletionWarning(ActionEvent event) {
        System.out.println("hey");
        if (this.deletionWarningListener != null) {
            this.deletionWarningListener.openDialog(new String[]{
                    "Are you sure you want to delete this tarmac?",
                    "This will delete both logical runways.",
                    "",
                    "This action is irreversible."
            }, (boolean result) -> {
                if (result) {
                    logger.info("Deleting runway {}.", this.runway.getRunwayDesignator());
                    if (this.deleteTarmacListener != null) {
                        System.out.println("Attempting tarmac deletion.");
                        this.deleteTarmacListener.attemptDeletion(this.runway);
                    } else {
                        System.out.println("No deleteTarmacListener!");
                    }
                } else {
                    System.out.println("Cancelled runway deletion.");
                }
            });
        }
    }

    /**
     * Perform the backend calculation and update this container.
     */
    public void recalculate() {
        logger.info("Attempting recalculation for runway {}", runway.getRunwayDesignator());
        this.parameterBox.resetValues();
        try {
            this.runway.recalculate(300);
            this.parameterBox.updateValues(this.runway.getCurrentValues());
        } catch (RunwayException | PositionException re) {
            logger.error(re.getStackTrace());
            logger.error("Could not recalculate runway parameters.");
        }
        updateVisual();
    }
    
    /**
     * Set the tarmac obstacle.
     * @param obstacle obstacle
     */
    public void setObstacle(Obstacle obstacle) {
        this.parameterBox.resetValues();
        this.runway.getTarmac().setObstacle(obstacle);
        this.disableRecalculation();
        this.obstacleBox.update(obstacle);
        logger.info("Set obstacle {} for runway {}.", obstacle.getName(), runway.getRunwayDesignator());
        this.updateVisual();
    }

    /**
     * Reset the parameters of the parameter box.
     */
    public void clearObstacle() {
        this.runway.getTarmac().removeObstacle();
        this.parameterBox.resetValues();
        this.disableRecalculation();
        this.obstacleBox.disablePositionFields();
        this.updateVisual();
    }

    /**
     * Bind view property from controller. True:Top, False:Side
     */
    public void bindViewProperty(BooleanProperty viewProperty) {
        this.topView.bind(viewProperty);
    }

    public void bindColourProperty(BooleanProperty viewProperty) {
        this.colour.bind(viewProperty);
    }

    /**
     * Call the correct update method in the runwayView object.
     */

    public void updateVisual() {
        logger.info("Updating view.");
        if (this.topView.get()) {
            this.runwayView.updateTopDown();
            this.runwayView.resetAngle();
        } else {
            this.runwayView.updateSideOn();
            this.runwayView.resetAngle();
        }
    }
    public void updateColour() {
        logger.info("Updating colour.");
        runwayView.bindColourProperty(this.colour);
        if (this.topView.get()) {
            this.runwayView.updateTopDown();
            this.runwayView.resetAngle();
        } else {
            this.runwayView.updateSideOn();
            this.runwayView.resetAngle();
        }

    }






    /**
     * Stop the user from being able to recalculate.
     */
    public void disableRecalculation() {
        this.updateVisual();
    }

    /**
     * Allow the user to attempt a recalculation.
     */
    public void enableRecalculation() {
        this.runway.reset();
        this.updateVisual();
        this.recalculate();
    }

    /**
     * Set the deletion listener for this runway.
     * @param listener listener
     */
    public void setDeleteTarmacListener(DeleteTarmacListener listener) {
        this.deleteTarmacListener = listener;
    }

    /**
     * Set the deletion warning listener for this runway.
     * @param listener listener
     */
    public void setDeletionWarningListener(WarningListener listener) {
        this.deletionWarningListener = listener;
    }
}
