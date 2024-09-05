/*
 * AISHA M. EL-GHAZALY
 * SEC. 5C-T2
 * 4200289
 * 
 * DIP TOOLBOX
 * 
 * */
package application;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Stack;

import javax.imageio.ImageIO;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class Controller implements Initializable{
	@FXML
	private ImageView imageView;
	
	@FXML
	private Slider brightnessSlider;
	
	@FXML
	private Slider contrastSlider;
	
	@FXML
	private Slider rotationSlider;
	
	@FXML
	private TextField scaleX, scaleY;
	
	@FXML
	private TextField roiX, roiY;
	
	@FXML
	private TextField transX, transY;
	
	@FXML
	private TextField gamma;
	
	@FXML
	private TextField low, high, value;
	
	@FXML
	private TextField threshold;
	
	@FXML 
	private TextField SP1, SP2, SP3, DP1, DP2, DP3;
	
	@FXML 
	private TextField cutoffFrequency;
	
	private Image ogImg;
	private Mat ogMat;
	private Image adjImg;
	private Mat adjMat;	
	private Stack<Mat> editStack;
    
    private String extension;
    
	static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
	
	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
	    // Initialize sliders to adjust image properties
	    brightnessSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
	        // Check if image and matrix are available
	        if (adjImg != null && adjMat != null) {
	            // Calculate the incremental change in brightness
	            int incrementalBrightness = newValue.intValue() - oldValue.intValue();
	            // Adjust image brightness and apply changes
	            Mat editedMat = adjustBrightness(adjMat, incrementalBrightness);
	            applyEdit(editedMat);
	        }
	    });

	    contrastSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
	        // Check if image and matrix are available
	        if (adjImg != null && adjMat != null) {
	            // Adjust image contrast based on slider value and apply changes
	            Mat editedMat = adjustContrast(ogMat, newValue.doubleValue());
	            applyEdit(editedMat);
	        }
	    });

	    // Initialize a stack to keep track of image editing history
	    editStack = new Stack<>();
	}

	/**
	 * Action event handler to add an image.
	 *
	 * @param event The event triggered by the Add button
	 */
	@FXML
	private void Add(ActionEvent event) {
	    FileChooser fileChooser = new FileChooser();
	    fileChooser.setTitle("Choose Image File");
	    fileChooser.getExtensionFilters().addAll(
	        new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
	    );

	    // Show open file dialog
	    File selectedFile = fileChooser.showOpenDialog(null);
	    if (selectedFile != null) {
	        // Get the selected image file path and extension
	        String imagePath = selectedFile.getAbsolutePath();
	        extension = imagePath.substring(imagePath.lastIndexOf(".") + 1);

	        // Read the image as a matrix
	        ogMat = Imgcodecs.imread(imagePath);
	        if (!ogMat.empty()) {
	            adjMat = new Mat();
	            ogMat.copyTo(adjMat);

	            // Convert to grayscale if image has multiple channels
	            if (ogMat.channels() > 1) {
	                Imgproc.cvtColor(adjMat, adjMat, Imgproc.COLOR_BGR2GRAY);
	            }

	            // Convert matrix to image for display
	            ogImg = mat2Image(ogMat, "." + extension);
	            adjImg = mat2Image(adjMat, "." + extension);
	            imageView.setImage(ogImg);

	            // Clear previous history when a new image is added
	            editStack.clear();
	            // Store the original image in the stack
	            editStack.push(new Mat(adjMat.size(), adjMat.type()));
	            adjMat.copyTo(editStack.peek());
	        } else {
	            System.out.println("Could not read the image");
	        }
	    }
	}
	
	/**
	 * Resets the image and UI elements to their original states.
	 *
	 * @param event The event triggered by the Reset button
	 */
	@FXML
	private void Reset(ActionEvent event) {
	    // Check if the original image and matrix exist
	    if (ogMat != null && ogImg != null) {
	        // Set the displayed image back to the original
	        imageView.setImage(ogImg);

	        // Reset sliders to their default positions
	        brightnessSlider.setValue(0);
	        contrastSlider.setValue(0);
	        rotationSlider.setValue(0);

	        // Clear text fields associated with image adjustments
	        scaleX.clear();
	        scaleY.clear();
	        roiX.clear();
	        roiY.clear();
	        transX.clear();
	        transY.clear();
	        gamma.clear();
	        low.clear();
	        high.clear();
	        value.clear();
	        threshold.clear();

	        // Clear SP and DP text fields (if applicable)
	        SP1.clear();
	        SP2.clear();
	        SP3.clear();
	        DP1.clear();
	        DP2.clear();
	        DP3.clear();

	        // Restore adjMat to the original state in ogMat
	        ogMat.copyTo(adjMat);

	        // Clear and re-store the original image in the editStack
	        editStack.clear();
	        editStack.push(new Mat(adjMat.size(), adjMat.type())); // Store the original image in the stack
	        adjMat.copyTo(editStack.peek());
	    } else {
	        System.out.println("No image loaded.");
	    }
	}

	/**
	 * Clears the displayed image and resets UI elements.
	 *
	 * @param event The event triggered by the Clear button
	 */
	@FXML
	private void Clear(ActionEvent event) {
	    // Clear the displayed image
	    imageView.setImage(null);

	    // Reset sliders to their default positions
	    brightnessSlider.setValue(0);
	    contrastSlider.setValue(0);
	    rotationSlider.setValue(0);

	    // Clear text fields associated with image adjustments
	    scaleX.clear();
	    scaleY.clear();
	    roiX.clear();
	    roiY.clear();
	    transX.clear();
	    transY.clear();
	    gamma.clear();
	    low.clear();
	    high.clear();
	    value.clear();
	    threshold.clear();

	    // Clear SP and DP text fields (if applicable)
	    SP1.clear();
	    SP2.clear();
	    SP3.clear();
	    DP1.clear();
	    DP2.clear();
	    DP3.clear();

	    // Clear the editStack
	    editStack.clear();
	}

	/**
	 * Displays statistics of the current image in a separate window.
	 *
	 * @param event The event triggered by the Show Image Statistics button
	 */
	@FXML
	private void showImageStatistics(ActionEvent event) {
	    // Clone the adjustment matrix to work with
	    Mat src = adjMat.clone();

	    // Labels to display various image statistics
	    Label rowsLabel = new Label("Image rows: " + src.rows());
	    Label colsLabel = new Label("Image cols: " + src.cols());
	    Label totalPixelsLabel = new Label("Image pixel total: " + src.total());
	    Label channelsLabel = new Label("Image channels: " + src.channels());
	    Label depthLabel = new Label("Image pixel depth: " + src.depth());

	    // Retrieve dimensions of the matrix
	    int rows = src.rows();
	    int cols = src.cols();

	    // Find minimum and maximum pixel values in the image
	    double[] firstPixelValue = src.get(0, 0);
	    double min = firstPixelValue[0];
	    double max = firstPixelValue[0];

	    for (int i = 0; i < rows; i++) {
	        for (int j = 0; j < cols; j++) {
	            double[] pixelValue = src.get(i, j);
	            double value = pixelValue[0];

	            if (value < min) {
	                min = value;
	            }

	            if (value > max) {
	                max = value;
	            }
	        }
	    }

	    // Labels to display minimum and maximum pixel values
	    Label minLabel = new Label("Min pixel value: " + (int) min);
	    Label maxLabel = new Label("Max pixel value: " + (int) max);

	    // Create a layout to display the image statistics
	    VBox statsLayout = new VBox(rowsLabel, colsLabel, totalPixelsLabel, channelsLabel, depthLabel, minLabel, maxLabel);

	    // Create a stage for the statistics display
	    Stage statsStage = new Stage();
	    statsStage.setTitle("Image Statistics");
	    statsStage.setScene(new Scene(statsLayout, 300, 250));
	    statsStage.show();

	    // Apply CSS styling to the labels for bold text and font family
	    String labelStyle = "-fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Arial'; -fx-text-fill: #333333;";
	    rowsLabel.setStyle(labelStyle);
	    colsLabel.setStyle(labelStyle);
	    totalPixelsLabel.setStyle(labelStyle);
	    channelsLabel.setStyle(labelStyle);
	    depthLabel.setStyle(labelStyle);
	    minLabel.setStyle(labelStyle);
	    maxLabel.setStyle(labelStyle);

	    // Apply CSS styling to the VBox layout
	    statsLayout.setStyle("-fx-background-color: #f4f4f4; -fx-padding: 20px;");
	}

	
	/**
	 * Applies histogram equalization to the loaded image.
	 *
	 * @param event The event triggered by the Equalize Histogram button
	 */
	@FXML
	private void Eq(ActionEvent event) {
	    // Check if the original image matrix is not empty
	    if (!ogMat.empty()) {
	        // Create a grayscale matrix if the image has multiple channels
	        Mat grayMat = new Mat();
	        if (adjMat.channels() > 1) {
	            Imgproc.cvtColor(adjMat, grayMat, Imgproc.COLOR_BGR2GRAY);
	        } else {
	            adjMat.copyTo(grayMat);
	        }

	        // Apply histogram equalization to the grayscale matrix
	        Mat editedMat = new Mat();
	        Imgproc.equalizeHist(grayMat, editedMat);

	        // Apply the edited matrix to the image
	        applyEdit(editedMat);
	    } else {
	        System.out.println("Could not read the image");
	    }
	}

	/**
	 * Applies negative transformation to the loaded image.
	 *
	 * @param event The event triggered by the Negative Transformation button
	 */
	@FXML
	private void Negative(ActionEvent event) {
	    // Check if the original image matrix is not empty
	    if (!ogMat.empty()) {
	        // Create a matrix filled with all 255 (white color)
	        Mat ones = new Mat(adjMat.size(), adjMat.type(), Scalar.all(255));

	        // Create a matrix to store the negative transformation result
	        Mat editedMat = new Mat();

	        // Subtract the original matrix from the ones matrix to obtain the negative
	        Core.subtract(ones, adjMat, editedMat);

	        // Apply the edited matrix to the image
	        applyEdit(editedMat);
	    } else {
	        System.out.println("Could not read the image");
	    }
	}

	
	/**
	 * Performs bit plane slicing on the loaded image and displays each bit plane in a separate window.
	 *
	 * @param event The event triggered by the Bit Plane Slicing button
	 */
	@FXML
	private void bitplaneSlicing(ActionEvent event) {
	    // Check if the adjusted matrix is not null
	    if (adjMat != null) {
	        // Create separate stages for each bit plane and corresponding image views
	        Stage[] stages = new Stage[8];
	        ImageView[] imageViews = new ImageView[8];

	        for (int i = 0; i < 8; i++) {
	            // Extract the bit plane by performing bitwise AND with a mask
	            Mat bitPlane = new Mat();
	            Core.bitwise_and(adjMat, new Mat(new Size(adjMat.cols(), adjMat.rows()), adjMat.type(), new Scalar(Math.pow(2, i))), bitPlane);

	            // Normalize the values to make them visually interpretable (0 and 255)
	            Core.multiply(bitPlane, new Scalar(255), bitPlane);
	            bitPlane.convertTo(bitPlane, CvType.CV_8U);

	            // Convert bit plane to JavaFX Image
	            Image image = mat2Image(bitPlane, extension);

	            // Create a new stage for displaying each bit plane
	            stages[i] = new Stage();
	            imageViews[i] = new ImageView(image);
	            stages[i].setTitle("Bit Plane " + i);
	            stages[i].setScene(new Scene(new Group(imageViews[i])));
	            stages[i].show();
	        }
	    } else {
	        System.out.println("No image loaded.");
	    }
	}
	
	/**
	 * Flips the image horizontally.
	 *
	 * @param event The event triggered by the Flip Horizontally button
	 */
	@FXML
	private void flipHorizontally(ActionEvent event) {
	    Core.flip(adjMat, adjMat, 1); // Flip horizontally
	    adjImg = mat2Image(adjMat, "." + extension);
	    imageView.setImage(adjImg);
	}

	/**
	 * Flips the image vertically.
	 *
	 * @param event The event triggered by the Flip Vertically button
	 */
	@FXML
	private void flipVertically(ActionEvent event) {
	    Core.flip(adjMat, adjMat, 0); // Flip vertically
	    adjImg = mat2Image(adjMat, "." + extension);
	    imageView.setImage(adjImg);
	}

	/**
	 * Rotates the image clockwise by 90 degrees.
	 *
	 * @param event The event triggered by the Rotate Clockwise button
	 */
	@FXML
	private void rotateClockwise(ActionEvent event) {
	    // Check if the adjusted matrix is not null
	    if (adjMat != null) {
	        Core.rotate(adjMat, adjMat, Core.ROTATE_90_CLOCKWISE);
	        adjImg = mat2Image(adjMat, extension);
	        imageView.setImage(adjImg);
	        updateRotationSliderValue(90); // Increment by 90 degrees
	    }
	}

	/**
	 * Rotates the image counter-clockwise by 90 degrees.
	 *
	 * @param event The event triggered by the Rotate Counter-Clockwise button
	 */
	@FXML
	private void rotateCounterClockwise(ActionEvent event) {
	    // Check if the adjusted matrix is not null
	    if (adjMat != null) {
	        Core.rotate(adjMat, adjMat, Core.ROTATE_90_COUNTERCLOCKWISE);
	        adjImg = mat2Image(adjMat, extension);
	        imageView.setImage(adjImg);
	        updateRotationSliderValue(-90); // Decrement by 90 degrees
	    }
	}
	// Variable to track previous rotation value for slider update
	private double previousRotationValue = 0;

	/**
	 * Updates the rotation slider value based on the provided angle.
	 *
	 * @param angle The angle to be added to the current rotation value
	 */
	private void updateRotationSliderValue(double angle) {
	    double currentRotation = rotationSlider.getValue();
	    double newRotation = currentRotation + angle;

	    // Ensure the rotation value stays within -360 to 360 range
	    if (newRotation > 360) {
	        newRotation -= 720;
	    } else if (newRotation < -360) {
	        newRotation += 720;
	    }

	    // Set the new value to the rotation slider
	    rotationSlider.setValue(newRotation);
	    // Update the previous rotation value
	    previousRotationValue = newRotation;
	}

	/**
	 * Scales the image based on the provided X and Y values and ROI coordinates.
	 *
	 * @param event The event triggered by the Scale Image button
	 */
	@FXML
	private void scaleImage(ActionEvent event) {
	    // Check if the adjusted matrix is not null
	    if (adjMat != null) {
	        // Get X and Y values from text fields
	        double newX = Double.parseDouble(scaleX.getText());
	        double newY = Double.parseDouble(scaleY.getText());

	        // Validate and adjust values to ensure they are within bounds
	        newX = Math.max(0, Math.min(adjMat.cols(), newX));
	        newY = Math.max(0, Math.min(adjMat.rows(), newY));

	        // Get the coordinates and dimensions of the ROI
	        int x = Integer.parseInt(roiX.getText());  // X-coordinate of top-left corner of ROI
	        int y = Integer.parseInt(roiY.getText());  // Y-coordinate of top-left corner of ROI
	        int width = (int) newX;  // Width of the ROI
	        int height = (int) newY; // Height of the ROI

	        // Ensure the ROI stays within the image bounds
	        x = Math.max(0, Math.min(adjMat.cols() - 1, x));
	        y = Math.max(0, Math.min(adjMat.rows() - 1, y));
	        width = Math.min(adjMat.cols() - x, width);
	        height = Math.min(adjMat.rows() - y, height);

	        // Extract the region of interest (ROI) from the original image
	        Mat srcZoom = new Mat(adjMat, new Rect(x, y, width, height));

	        // Resize the extracted region
	        Mat dstZoom = new Mat();
	        Size newSize = new Size();  // Size will be calculated automatically
	        Imgproc.resize(srcZoom, dstZoom, newSize, 2, 2, Imgproc.INTER_LINEAR);

	        // Update the displayed image with the scaled region
	        applyEdit(dstZoom);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}
	
	/**
	 * Translates the image based on the provided X and Y translation values.
	 *
	 * @param event The event triggered by the Translate Image button
	 */
	@FXML
	private void translateImage(ActionEvent event) {
	    // Check if the adjusted matrix is not null
	    if (adjMat != null) {
	        // Get the X and Y translation values from text fields
	        int translationX = Integer.parseInt(transX.getText());
	        int translationY = Integer.parseInt(transY.getText());

	        // Create a translation matrix
	        Mat translationMatrix = Mat.eye(2, 3, CvType.CV_32F);
	        translationMatrix.put(0, 2, translationX); // Update the X translation
	        translationMatrix.put(1, 2, translationY); // Update the Y translation

	        // Apply the translation to the image
	        Mat translatedMat = new Mat();
	        Imgproc.warpAffine(adjMat, translatedMat, translationMatrix, adjMat.size());

	        // Update the displayed image with the translated image
	        adjImg = mat2Image(translatedMat, extension);
	        imageView.setImage(adjImg);

	        // Apply the edit and update the history stack
	        applyEdit(translatedMat);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}

	/**
	 * Performs an affine transformation based on provided source and destination points.
	 *
	 * @param event The event triggered by the Perform Affine Transformation button
	 */
	@FXML
	private void performAffineTransformation(ActionEvent event) {
	    // Check if the adjusted matrix is not null
	    if (adjMat != null) {
	        Point[] srcPoints = new Point[3];
	        Point[] dstPoints = new Point[3];

	        // Extract source and destination points from text fields
	        srcPoints[0] = extractPointFromTextField(SP1);
	        srcPoints[1] = extractPointFromTextField(SP2);
	        srcPoints[2] = extractPointFromTextField(SP3);

	        dstPoints[0] = extractPointFromTextField(DP1);
	        dstPoints[1] = extractPointFromTextField(DP2);
	        dstPoints[2] = extractPointFromTextField(DP3);

	        // Check if points are valid and not null
	        if (srcPoints[0] != null && srcPoints[1] != null && srcPoints[2] != null &&
	            dstPoints[0] != null && dstPoints[1] != null && dstPoints[2] != null) {

	            // Convert points to MatOfPoint2f format
	            MatOfPoint2f srcMat = new MatOfPoint2f(srcPoints);
	            MatOfPoint2f dstMat = new MatOfPoint2f(dstPoints);

	            // Get the affine transformation matrix
	            Mat transformationMatrix = Imgproc.getAffineTransform(srcMat, dstMat);

	            // Apply the affine transformation to the image
	            Mat transformedMat = new Mat();
	            Imgproc.warpAffine(adjMat, transformedMat, transformationMatrix, adjMat.size());

	            // Update the displayed image with the transformed image
	            adjImg = mat2Image(transformedMat, extension);
	            imageView.setImage(adjImg);

	            // Apply the edit and update the history stack
	            applyEdit(transformedMat);
	        } else {
	            System.out.println("Invalid points entered.");
	        }
	    } else {
	        System.out.println("No image loaded.");
	    }
	}
	
	/**
	 * Performs gamma correction on the image based on the provided gamma value.
	 *
	 * @param event The event triggered by the Gamma Correction button
	 */
	@FXML
	private void gammaCorrection(ActionEvent event) {
	    // Check if the adjusted matrix is not null
	    if (adjMat != null) {
	        try {
	            // Get gamma value from a text field
	            double gammaValue = Double.parseDouble(gamma.getText()); // Replace gammaTextField with your text field

	            // Perform gamma correction on the image
	            Mat correctedImage = performGammaCorrection(adjMat, gammaValue);

	            if (correctedImage != null) {
	                applyEdit(correctedImage);
	            } else {
	                System.out.println("Failed to perform gamma correction.");
	            }
	        } catch (NumberFormatException e) {
	            System.out.println("Invalid gamma value entered.");
	        }
	    } else {
	        System.out.println("No image loaded.");
	    }
	}

	/**
	 * Performs grey level slicing on the image based on provided threshold values and chosen intensity.
	 *
	 * @param event The event triggered by the Grey Level Slicing button
	 */
	@FXML
	private void performGreyLevelSlicing(ActionEvent event) {
	    // Check if the adjusted matrix is not null
	    if (adjMat != null) {
	        try {
	            int lowThreshold = Integer.parseInt(low.getText());
	            int highThreshold = Integer.parseInt(high.getText());
	            int chosenValue = Integer.parseInt(value.getText());

	            // Validate threshold values
	            if (lowThreshold < 0 || highThreshold > 255 || lowThreshold >= highThreshold) {
	                System.out.println("Invalid threshold values. Low should be less than high and both within [0, 255] range.");
	                return;
	            }

	            Mat editedMat = new Mat();

	            if (adjMat.channels() == 1) { // Grayscale image
	                editedMat = adjMat.clone(); // Clone the original image

	                // Apply grey level slicing on grayscale image
	                for (int i = 0; i < editedMat.rows(); i++) {
	                    for (int j = 0; j < editedMat.cols(); j++) {
	                        double pixelValue = editedMat.get(i, j)[0];
	                        if (pixelValue > lowThreshold && pixelValue < highThreshold) {
	                            // Highlight the pixel within the range
	                            editedMat.put(i, j, chosenValue); // Set a different intensity value
	                        }
	                    }
	                }
	            } else { // Color image, convert to grayscale and then apply grey level slicing
	                Mat grayMat = new Mat();
	                Imgproc.cvtColor(adjMat, grayMat, Imgproc.COLOR_BGR2GRAY);

	                editedMat = grayMat.clone(); // Clone the grayscale image

	                // Apply grey level slicing on grayscale image
	                for (int i = 0; i < editedMat.rows(); i++) {
	                    for (int j = 0; j < editedMat.cols(); j++) {
	                        double pixelValue = editedMat.get(i, j)[0];
	                        if (pixelValue > lowThreshold && pixelValue < highThreshold) {
	                            // Highlight the pixel within the range
	                            editedMat.put(i, j, chosenValue); // Set a different intensity value
	                        }
	                    }
	                }
	            }

	            // Apply the edited matrix and update the history stack
	            applyEdit(editedMat);
	        } catch (NumberFormatException e) {
	            System.out.println("Invalid threshold values entered.");
	        }
	    } else {
	        System.out.println("No image loaded.");
	    }
	}
	
	/**
	 * Applies an average filter (3x3 kernel) to the loaded image.
	 *
	 * @param event The event triggered by the Apply Average Filter button
	 */
	@FXML
	private void applyAverageFilter(ActionEvent event) {
	    if (adjMat != null) {
	        Mat filteredMat = new Mat();

	        Mat kernel = Mat.ones(3, 3, CvType.CV_32F);
	        Core.divide(kernel, new Scalar(9), kernel); // Divide by 9 for average

	        Imgproc.filter2D(adjMat, filteredMat, -1, kernel);

	        applyEdit(filteredMat);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}

	/**
	 * Applies a weighted filter (3x3 kernel) to the loaded image.
	 *
	 * @param event The event triggered by the Apply Weighted Filter button
	 */
	@FXML
	private void applyWeightedFilter(ActionEvent event) {
	    if (adjMat != null) {
	        Mat filteredMat = new Mat();

	        Mat kernel = new Mat(3, 3, CvType.CV_32F);
	        float[] data = {1, 2, 1, 2, 4, 2, 1, 2, 1};
	        kernel.put(0, 0, data);
	        Core.divide(kernel, new Scalar(16), kernel); // Normalize by dividing by the sum

	        Imgproc.filter2D(adjMat, filteredMat, -1, kernel);

	        applyEdit(filteredMat);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}

	/**
	 * Applies a circular filter (5x5 kernel) to the loaded image.
	 *
	 * @param event The event triggered by the Apply Circular Filter button
	 */
	@FXML
	private void applyCircularFilter(ActionEvent event) {
	    if (adjMat != null) {
	        Mat filteredMat = new Mat();

	        Mat kernel = new Mat(5, 5, CvType.CV_32F);
	        float[] data = {
	                0, 0, 1, 0, 0,
	                0, 1, 1, 1, 0,
	                1, 1, 1, 1, 1,
	                0, 1, 1, 1, 0,
	                0, 0, 1, 0, 0
	        };
	        kernel.put(0, 0, data);
	        Core.divide(kernel, new Scalar(13), kernel); // Normalize by dividing by the sum

	        Imgproc.filter2D(adjMat, filteredMat, -1, kernel);

	        applyEdit(filteredMat);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}

	/**
	 * Applies a median filter (5x5 kernel) to the loaded image.
	 *
	 * @param event The event triggered by the Apply Median Filter button
	 */
	@FXML
	private void applyMedianFilter(ActionEvent event) {
	    if (adjMat != null) {
	        Mat filteredMat = new Mat();

	        int kernelSize = 5;

	        Imgproc.medianBlur(adjMat, filteredMat, kernelSize);

	        applyEdit(filteredMat);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}
	
	/**
	 * Applies Sobel edge detection to the loaded image.
	 *
	 * @param event The event triggered by the Apply Sobel Edge Detection button
	 */
	@FXML
	private void applySobelEdgeDetection(ActionEvent event) {
	    if (adjMat != null) {
	        Mat src = adjMat.clone();

	        Mat dstH = new Mat();
	        Mat dstV = new Mat();
	        Mat dst = new Mat();

	        // Horizontal Sobel Kernel
	        Mat kernelH = new Mat(3, 3, CvType.CV_32S);
	        kernelH.put(0, 0, -1, -2, -1, 0, 0, 0, 1, 2, 1);

	        Imgproc.filter2D(src, dstH, CvType.CV_16S, kernelH);
	        Core.convertScaleAbs(dstH, dstH);

	        // Vertical Sobel Kernel
	        Mat kernelV = new Mat(3, 3, CvType.CV_32S);
	        kernelV.put(0, 0, -1, 0, 1, -2, 0, 2, -1, 0, 1);

	        Imgproc.filter2D(src, dstV, CvType.CV_16S, kernelV);
	        Core.convertScaleAbs(dstV, dstV);

	        Core.addWeighted(dstH, 1, dstV, 1, 0, dst);

	        // Display the resultant image
	        Image edgeImage = mat2Image(dst, "." + extension);
	        imageView.setImage(edgeImage);

	        // Store the resultant image in the edit stack
	        applyEdit(dst);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}

	/**
	 * Applies Laplacian sharpening to the loaded image.
	 *
	 * @param event The event triggered by the Apply Laplacian Sharpening button
	 */
	@FXML
	private void applyLaplacianSharpening(ActionEvent event) {
	    if (adjMat != null) {
	        Mat src = adjMat.clone();

	        // Apply Gaussian blur to reduce noise before Laplacian filtering
	        Mat blurred = new Mat();
	        Imgproc.GaussianBlur(src, blurred, new Size(3, 3), 0);

	        // Apply Laplacian filter to highlight edges
	        Mat laplacian = new Mat();
	        Imgproc.Laplacian(blurred, laplacian, CvType.CV_16S);

	        // Convert back to CV_8U for display purposes
	        Mat sharpened = new Mat();
	        Core.convertScaleAbs(laplacian, sharpened);

	        // Combine original image with sharpened edges using addWeighted
	        Mat result = new Mat();
	        Core.addWeighted(src, 1, sharpened, 1, 0, result);

	        // Display the resultant image
	        Image sharpenedImage = mat2Image(result, "." + extension);
	        imageView.setImage(sharpenedImage);

	        // Store the resultant image in the edit stack
	        applyEdit(result);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}

    
	/**
	 * Performs edge detection on the loaded image using a custom Laplacian filter.
	 * This method enhances the edges of the image by applying a specific filter.
	 *
	 * @param event The event triggered by the Perform Edge Detection button
	 */
	@FXML
	private void performEdgeDetection(ActionEvent event) {
	    if (adjMat != null) {
	        Mat src = adjMat.clone();
	        Mat dstL = new Mat();

	        // Define a custom Laplacian kernel
	        Mat kernelL = new Mat(3, 3, CvType.CV_32F);
	        kernelL.put(0, 0, -1, -1, -1, -1, 8, -1, -1, -1, -1);

	        // Apply the custom Laplacian filter
	        Imgproc.filter2D(src, dstL, CvType.CV_8UC1, kernelL);

	        // Display the resultant image
	        applyEdit(dstL);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}

	/**
	 * Performs threshold segmentation on the loaded image.
	 * This method segments the image based on a specified threshold value.
	 *
	 * @param event The event triggered by the Perform Threshold Segmentation button
	 */
	@FXML
	private void performThresholdSegmentation(ActionEvent event) {
	    if (adjMat != null) {
	        // Get threshold value from the text field
	        int thresholdValue = Integer.parseInt(threshold.getText());

	        Mat src = adjMat.clone();
	        Mat dstT = new Mat(src.rows(), src.cols(), CvType.CV_8UC1);

	        for (int r = 0; r < src.rows(); r++) {
	            for (int c = 0; c < src.cols(); c++) {
	                double pixelValue = src.get(r, c)[0];
	                if (pixelValue > thresholdValue) {
	                    dstT.put(r, c, 255); // Set high intensity for pixels above threshold
	                } else {
	                    dstT.put(r, c, 0); // Set low intensity for pixels below threshold
	                }
	            }
	        }

	        // Display the resultant image
	        applyEdit(dstT);
	    } else {
	        System.out.println("No image loaded.");
	    }
	}
	
	/**
	 * Adjusts the brightness of the input image matrix.
	 *
	 * @param inputMat        The input image matrix
	 * @param brightnessValue The brightness adjustment value
	 * @return The image matrix with adjusted brightness
	 */
	private Mat adjustBrightness(Mat inputMat, int brightnessValue) {
	    Mat outputMat = new Mat();
	    inputMat.convertTo(outputMat, -1, 1, brightnessValue);
	    return outputMat;
	}

	/**
	 * Adjusts the contrast of the input image matrix.
	 *
	 * @param inputMat      The input image matrix
	 * @param contrastValue The contrast adjustment value
	 * @return The image matrix with adjusted contrast
	 */
	private Mat adjustContrast(Mat inputMat, double contrastValue) {
	    Mat outputMat = new Mat();
	    inputMat.convertTo(outputMat, -1, contrastValue, 0);
	    return outputMat;
	}

	/**
	 * Performs gamma correction on the input image matrix.
	 *
	 * @param inputMat The input image matrix
	 * @param gamma    The gamma value for correction
	 * @return The image matrix with gamma correction applied
	 */
	private Mat performGammaCorrection(Mat inputMat, double gamma) {
	    Mat correctedImage = new Mat(inputMat.rows(), inputMat.cols(), CvType.CV_32FC1);

	    for (int i = 0; i < inputMat.rows(); i++) {
	        for (int j = 0; j < inputMat.cols(); j++) {
	            double[] pixelValue = inputMat.get(i, j);
	            double correctedValue = Math.pow(pixelValue[0], gamma);
	            correctedImage.put(i, j, correctedValue);
	        }
	    }

	    Core.normalize(correctedImage, correctedImage, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);
	    Core.convertScaleAbs(correctedImage, correctedImage);

	    return correctedImage;
	}
    
	/**
	 * Extracts a point from the provided TextField.
	 *
	 * @param textField The TextField containing the point coordinates
	 * @return The extracted point
	 */
	private Point extractPointFromTextField(TextField textField) {
	    try {
	        String text = textField.getText();
	        String[] values = text.split(",");

	        if (values.length == 2) {
	            double x = Double.parseDouble(values[0].trim());
	            double y = Double.parseDouble(values[1].trim());
	            return new Point(x, y);
	        } else {
	            System.out.println("Invalid input format for point.");
	        }
	    } catch (NumberFormatException e) {
	        System.out.println("Invalid number format for point.");
	    }
	    return null;
	}

	/**
	 * Apply an edit to the image and update the history stack.
	 *
	 * @param editedMat The edited image matrix to be applied
	 */
	private void applyEdit(Mat editedMat) {
	    if (adjMat != null) {
	        Mat edited = new Mat();
	        adjMat.copyTo(edited); // Create a copy of the current adjusted image
	        editStack.push(edited); // Store a copy of the current state before editing
	        editedMat.copyTo(adjMat); // Apply the new edit
	        adjImg = mat2Image(adjMat, "." + extension);
	        imageView.setImage(adjImg);
	    }
	}

	/**
	 * Converts a Mat object from OpenCV to JavaFX Image.
	 *
	 * @param mat    The Mat object to convert
	 * @param format The format of the image
	 * @return The converted JavaFX Image
	 */
	private Image mat2Image(Mat mat, String format) {
	    MatOfByte byteMat = new MatOfByte();
	    boolean success = Imgcodecs.imencode("." + format, mat, byteMat);
	    if (!success) {
	        System.out.println("Error occurred while encoding the image.");
	        return null;
	    }

	    byte[] byteArray = byteMat.toArray();

	    try {
	        BufferedImage bufImage = ImageIO.read(new ByteArrayInputStream(byteArray));
	        return SwingFXUtils.toFXImage(bufImage, null);
	    } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	    }
	}

    
	/*
	 * @FXML private void applyFrequencyFilter(ActionEvent event) { if (adjMat !=
	 * null) { int de = Integer.parseInt(cutoffFrequency.getText()); // Get cutoff
	 * frequency from a text field
	 * 
	 * Mat padded = new Mat(); Core.copyMakeBorder(adjMat, padded, 0,
	 * Core.getOptimalDFTSize(adjMat.rows()), 0,
	 * Core.getOptimalDFTSize(adjMat.cols()) - adjMat.cols(), Core.BORDER_CONSTANT,
	 * Scalar.all(0));
	 * 
	 * padded.convertTo(padded, CvType.CV_32FC1, 1.0 / 255.0);
	 * 
	 * List<Mat> planes = new ArrayList<>(); planes.add(padded);
	 * planes.add(Mat.zeros(padded.size(), CvType.CV_32FC1));
	 * 
	 * Mat complexI = new Mat(); Core.merge(planes, complexI);
	 * 
	 * Core.dft(complexI, complexI);
	 * 
	 * int cx = complexI.cols() / 2; int cy = complexI.rows() / 2;
	 * 
	 * Mat LFilter = Mat.zeros(complexI.size(), CvType.CV_32FC1);
	 * 
	 * for (int i = 0; i < LFilter.rows(); i++) { for (int j = 0; j <
	 * LFilter.cols(); j++) { double z1 = i - LFilter.rows() / 2; double z2 = j -
	 * LFilter.cols() / 2; if (Math.sqrt(Math.pow(z1, 2) + Math.pow(z2, 2)) <= de)
	 * LFilter.put(i, j, 1); else LFilter.put(i, j, 0); } }
	 * 
	 * List<Mat> planesList = new ArrayList<>(); Core.split(complexI.submat(new
	 * Rect(0, 0, cx, cy)), planesList);
	 * planesList.add(Mat.zeros(complexI.submat(new Rect(0, 0, cx, cy)).size(),
	 * CvType.CV_32FC1));
	 * 
	 * Mat[] planesArray = planesList.toArray(new Mat[0]);
	 * 
	 * Core.multiply(planesArray[0], LFilter.submat(new Rect(0, 0, cx, cy)),
	 * planesArray[0]); Core.multiply(planesArray[1], LFilter.submat(new Rect(0, 0,
	 * cx, cy)), planesArray[1]);
	 * 
	 * Mat filteredComplexI = new Mat(); Core.merge(Arrays.asList(planesArray),
	 * filteredComplexI);
	 * 
	 * Core.idft(filteredComplexI, filteredComplexI);
	 * 
	 * List<Mat> outPlanes = new ArrayList<>(); Core.split(filteredComplexI,
	 * outPlanes);
	 * 
	 * Mat filteredImage = new Mat(); Core.magnitude(outPlanes.get(0),
	 * outPlanes.get(1), filteredImage); Core.normalize(filteredImage,
	 * filteredImage, 1, 0, Core.NORM_MINMAX);
	 * 
	 * applyEdit(filteredImage); // Update the displayed image with the filtered
	 * result } else { System.out.println("No image loaded."); } }
	 */
    
    /**
     * Undo the last edit made to the image by reverting to the previous state.
     * This method utilizes an edit stack to keep track of image edits and allows undoing changes.
     *
     * @param event The event triggered by the Undo button
     */
    @FXML
    private void onUndo(ActionEvent event) {
        if (!editStack.isEmpty()) {
            editStack.pop(); // Remove the current state

            if (!editStack.isEmpty()) {
                // Revert to the previous state
                editStack.peek().copyTo(adjMat);
                adjImg = mat2Image(adjMat, "." + extension);
                imageView.setImage(adjImg);
            } else {
                // If the stack is empty, reset to the original image
                imageView.setImage(ogImg);
                ogMat.copyTo(adjMat);
                editStack.push(new Mat(adjMat.size(), adjMat.type())); // Store the original image in the stack
                adjMat.copyTo(editStack.peek());
            }
        }
    }

    /**
     * Save the currently displayed image.
     * This method allows users to save the currently displayed image to their local storage.
     *
     * @param event The event triggered by the Save Image button
     */
    @FXML
    private void saveImage(ActionEvent event) {
        if (adjMat != null) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Image");
            fileChooser.getExtensionFilters().addAll(
                new ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
            );

            // Show save file dialog
            File file = fileChooser.showSaveDialog(null);
            if (file != null) {
                String imagePath = file.getAbsolutePath();
                Imgcodecs.imwrite(imagePath, adjMat);
            }
        } else {
            System.out.println("No image loaded.");
        }
    }
}