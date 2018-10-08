/*
 * Copyright 2018 Esri France.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.esrifrance.sig2018.scene.scene_layer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TileCache;
import com.esri.arcgisruntime.geoanalysis.LocationViewshed;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.layers.ArcGISSceneLayer;
import com.esri.arcgisruntime.layers.ArcGISTiledLayer;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Bookmark;
import com.esri.arcgisruntime.mapping.BookmarkList;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.AnalysisOverlay;
import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.tasks.geocode.GeocodeParameters;
import com.esri.arcgisruntime.tasks.geocode.GeocodeResult;
import com.esri.arcgisruntime.tasks.geocode.LocatorTask;
import com.esri.arcgisruntime.tasks.geocode.ReverseGeocodeParameters;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LocationViewshedDemo extends Application {

	private ArcGISScene scene;
	private SceneView sceneView;
	private StackPane stackPane;

	// Bookmark
	private BookmarkList bookmarkList;
	private Bookmark bookmark;

	// Viewshed
	private LocationViewshed viewshed;
	private AnalysisOverlay analysisOverlay;

	// Locator
	private LocatorTask locatorTask;
	private GeocodeParameters geocodeParameters;
	private GraphicsOverlay locatorOverlay;
	private PictureMarkerSymbol pinSymbol;
	private ComboBox<String> searchBox;
	private ReverseGeocodeParameters reverseGeocodeParameters;

	private void createScene() {
		// create a scene and add a basemap to it
		scene = new ArcGISScene();

		TileCache tileCache = new TileCache("./samples-data/sig2018/Paris.tpk");
		ArcGISTiledLayer tiledLayer = new ArcGISTiledLayer(tileCache);

		scene.setBasemap(new Basemap(tiledLayer));

		// add the SceneView to the stack pane
		sceneView = new SceneView();
		sceneView.setArcGISScene(scene);
		stackPane.getChildren().addAll(sceneView);

		// add base surface for elevation data
		Surface surface = new Surface();
		// final String elevationImageService =
		// "https://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/TopoBathy3D/ImageServer";

		final String elevationRaster = "./samples-data/sig2018/Terrain3D/v101/D_RGE_ALTI_01m_S12017_v3";
		surface.getElevationSources().add(new ArcGISTiledElevationSource(elevationRaster));
		scene.setBaseSurface(surface);

		// add a scene layer
		// final String buildings =
		// "http://tiles.arcgis.com/tiles/P3ePLMYs2RVChkJx/arcgis/rest/services/Buildings_Brest/SceneServer/layers/0";
		final String buildings = "./samples-data/sig2018/Bati3D_Paris.spk";
		ArcGISSceneLayer sceneLayer = new ArcGISSceneLayer(buildings);
		scene.getOperationalLayers().add(sceneLayer);

		// add a camera and initial camera position (Paris, France)
		Camera camera = new Camera(48.85, 2.35, 1000.0, 10.0, 70, 0.0);
		sceneView.setViewpointCamera(camera);
	}

	private void createBookmarks() {
		// create a control panel
		VBox controlsVBox = new VBox(6);
		controlsVBox.setBackground(
				new Background(new BackgroundFill(Paint.valueOf("rgba(0,0,0,0.3)"), CornerRadii.EMPTY, Insets.EMPTY)));
		controlsVBox.setPadding(new Insets(10.0));
		controlsVBox.setMaxSize(220, 240);
		controlsVBox.getStyleClass().add("panel-region");

		// create label for bookmarks
		Label bookmarkLabel = new Label("Bookmarks");
		bookmarkLabel.getStyleClass().add("panel-label");

		// create a list to hold the names of the bookmarks
		ListView<String> bookmarkNames = new ListView<>();
		bookmarkNames.setMaxHeight(190);

		// when user clicks on a bookmark change to that location
		bookmarkNames.getSelectionModel().selectedItemProperty().addListener((ov, old_val, new_val) -> {
			int index = bookmarkNames.getSelectionModel().getSelectedIndex();
			sceneView.setViewpoint(bookmarkList.get(index).getViewpoint());
		});

		// create button to add a bookmark
		Button addBookmarkButton = new Button("Add Bookmark");
		addBookmarkButton.setMaxWidth(Double.MAX_VALUE);

		// show dialog to user and add a bookmark if text is entered
		addBookmarkButton.setOnAction(e -> {
			TextInputDialog dialog = new TextInputDialog();
			dialog.setHeaderText("Add Bookmark");
			dialog.setContentText("Bookmark Name");

			Optional<String> result = dialog.showAndWait();
			result.ifPresent(text -> {
				if (text.trim().length() > 0 && !bookmarkNames.getItems().contains(text)) {
					bookmark = new Bookmark(text, sceneView.getCurrentViewpoint(Viewpoint.Type.BOUNDING_GEOMETRY));
					bookmarkList.add(bookmark);
					bookmarkNames.getItems().add(bookmark.getName());
				} else {
					Alert alert = new Alert(AlertType.INFORMATION);
					alert.setHeaderText("Text Error");
					alert.setContentText("Text name already exist or no text was entered.");
					alert.showAndWait();
				}
			});
		});

		// add label and bookmarks to the control panel
		controlsVBox.getChildren().addAll(bookmarkLabel, bookmarkNames, addBookmarkButton);

		// get all the bookmarks from the ArcGISMap
		bookmarkList = scene.getBookmarks();

		// add default bookmarks
		Viewpoint viewpoint;

		viewpoint = new Viewpoint(48.858201, 2.294653, 6e3);
		bookmark = new Bookmark("Tour Eiffel", viewpoint);
		bookmarkList.add(bookmark);
		bookmarkNames.getItems().add(bookmark.getName());
		// set this bookmark as the ArcGISMap's initial viewpoint
		// sceneView.setViewpointAsync(viewpoint);

		viewpoint = new Viewpoint(48.873799, 2.295017, 6e3);
		bookmark = new Bookmark("Arc de Triomphe", viewpoint);
		bookmarkNames.getItems().add(bookmark.getName());
		bookmarkList.add(bookmark);

		viewpoint = new Viewpoint(48.853174, 2.369118, 6e3);
		bookmark = new Bookmark("La Bastille", viewpoint);
		bookmarkNames.getItems().add(bookmark.getName());
		bookmarkList.add(bookmark);

		viewpoint = new Viewpoint(48.853032, 2.349956, 6e3);
		bookmark = new Bookmark("Notre Dame", viewpoint);
		bookmarkNames.getItems().add(bookmark.getName());
		bookmarkList.add(bookmark);

		// add the map view and control panel to stack pane
		stackPane.getChildren().addAll(controlsVBox);
		StackPane.setAlignment(controlsVBox, Pos.TOP_LEFT);
		StackPane.setMargin(controlsVBox, new Insets(10, 0, 0, 10));
	}
	
	
	private void createViewshed() {
		viewshed = new LocationViewshed(this.sceneView.getCurrentViewpointCamera(), 10.0, 2000.0);
		viewshed.setFrustumOutlineVisible(true);

		// create an analysis overlay to add the viewshed to the scene view
		analysisOverlay = new AnalysisOverlay();
		analysisOverlay.getAnalyses().add(viewshed);
		//sceneView.getAnalysisOverlays().add(analysisOverlay);
	}

	private void computeViewshed() {
		// create a viewshed from the camera
		if (!sceneView.getAnalysisOverlays().contains(analysisOverlay)) {
			sceneView.getAnalysisOverlays().add(analysisOverlay);
		}

		viewshed.updateFromCamera(sceneView.getCurrentViewpointCamera());
	}

	private void createLocator() {
		

		// create a locator task
		final String locatorPath = new File("./samples-data/sig2018/BAN_France/v101/BAN_France.loc").getAbsolutePath();
		locatorTask = new LocatorTask(locatorPath);

		// set geocode task parameters
		geocodeParameters = new GeocodeParameters();
		geocodeParameters.getResultAttributeNames().add("*"); // return all attributes
		geocodeParameters.setMaxResults(1); // get closest match

		// set reverse geocode task parameters
		reverseGeocodeParameters = new ReverseGeocodeParameters();
		reverseGeocodeParameters.getResultAttributeNames().add("*");
		reverseGeocodeParameters.setOutputSpatialReference(sceneView.getSpatialReference());
		
		// create search box
		searchBox = new ComboBox<>();
		searchBox.setPromptText("Search");
		searchBox.setEditable(true);
		searchBox.setMaxWidth(260.0);
				
		// event to get geocode when query is submitted
		searchBox.setOnAction((e) -> {

			// get the user's query
			String query;
			if (searchBox.getSelectionModel().getSelectedIndex() == -1) {
				// user supplied their own query
				query = searchBox.getEditor().getText() + " Paris";
			} else {
				// user chose a suggested query
				query = searchBox.getSelectionModel().getSelectedItem();
			}
			
			 geocode(query);

		});

		// add a graphics overlay
		locatorOverlay = new GraphicsOverlay();
		locatorOverlay.setSelectionColor(0xFF00FFFF);
		sceneView.getGraphicsOverlays().add(locatorOverlay);

		// create a pin graphic
		Image img = new Image(getClass().getResourceAsStream("/symbols/pin.png"), 0, 80, true, true);
		pinSymbol = new PictureMarkerSymbol(img);
		pinSymbol.loadAsync();

		// add map view and searchBox to stack pane
		stackPane.getChildren().addAll(searchBox);
		StackPane.setAlignment(searchBox, Pos.TOP_RIGHT);
		StackPane.setMargin(searchBox, new Insets(10, 0, 0, 10));

	}

	private void geocode(String query) {
		if (query.equals("")) {
			return;
		}

		// hide callout if showing
		sceneView.getCallout().dismiss();

		// run the locatorTask geocode task
		ListenableFuture<List<GeocodeResult>> results = locatorTask.geocodeAsync(query, geocodeParameters);

		// add a listener to display the result when loaded
		results.addDoneListener(() -> displayGeocodeResults(results, false));

	}

	private void reverseGeocode(Point point) {
		// reverse geocode the selected point
		ListenableFuture<List<GeocodeResult>> results = locatorTask.reverseGeocodeAsync(point,
				reverseGeocodeParameters);

		// add a listener to display the result when loaded
		results.addDoneListener(() -> displayGeocodeResults(results, true));
	}

	private void displayGeocodeResults(ListenableFuture<List<GeocodeResult>> results, boolean reverse) {

		List<GeocodeResult> geocodes = null;
		try {
			geocodes = results.get();
		} catch (Exception e) {}
		
		if (geocodes == null || geocodes.size() == 0) {
			return;
		}
		// get the top result
		GeocodeResult geocode = geocodes.get(0);

		// set the viewpoint to the marker
		Point location = geocode.getDisplayLocation();
		if(!reverse) {
			sceneView.setViewpointAsync(new Viewpoint(location, 6e3));
		}

		// get attributes from the result for the callout
		String title;
		String detail;
		Object matchAddr = geocode.getAttributes().get("Match_addr");
		if (matchAddr != null) {
			// attributes from a query-based search
			title = matchAddr.toString().split(",")[0];
			detail = matchAddr.toString().substring(matchAddr.toString().indexOf(",") + 1);
		} else {
			// attributes from a click-based search
			String street = geocode.getAttributes().get("Street").toString();
			String city = geocode.getAttributes().get("City").toString();
			String state = geocode.getAttributes().get("State").toString();
			String zip = geocode.getAttributes().get("ZIP").toString();
			title = street;
			detail = city + ", " + state + " " + zip;
		}

		// get attributes from the result for the callout
		HashMap<String, Object> attributes = new HashMap<>();
		attributes.put("title", title);
		attributes.put("detail", detail);

		// create the marker
		Graphic marker = new Graphic(geocode.getDisplayLocation(), attributes, pinSymbol);

		// update the callout
		Platform.runLater(() -> {
			// clear out previous results
			locatorOverlay.getGraphics().clear();

			// add the markers to the graphics overlay
			locatorOverlay.getGraphics().add(marker);

			// display the callout
			Callout callout = sceneView.getCallout();
			callout.setTitle(marker.getAttributes().get("title").toString());
			callout.setDetail(marker.getAttributes().get("detail").toString());
			callout.showCalloutAt(location, new Point2D(0, -24), Duration.ZERO);
		});

	}

	@Override
	public void start(Stage stage) {

		try {

			// create stack pane and JavaFX app scene
			stackPane = new StackPane();
			Scene fxScene = new Scene(stackPane);

			// set title, size, and add JavaFX scene to stage
			stage.setTitle("Paris Scene Layer");
			stage.setWidth(800);
			stage.setHeight(700);
			stage.setScene(fxScene);
			stage.show();

			createScene();
			createBookmarks();
			createViewshed();
			createLocator();

			sceneView.setOnMouseClicked(e -> {
				if(!e.isStillSincePress()) {
					return;
				}
				
				if (e.getButton() == MouseButton.SECONDARY) {
					analysisOverlay.getAnalyses().clear();
					locatorOverlay.getGraphics().clear();
				}else if ( e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
					computeViewshed();
				}else if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 1) {
					Point2D clickLocation = new Point2D(e.getX(), e.getY());
					Point point = sceneView.screenToBaseSurface(clickLocation);
					reverseGeocode(point);
				}
			});

		} catch (Exception e) {
			// on any error, display the stack trace.
			e.printStackTrace();
		}
	}

	/**
	 * Stops and releases all resources used in application.
	 */
	@Override
	public void stop() {

		if (sceneView != null) {
			sceneView.dispose();
		}
	}

	/**
	 * Opens and runs application.
	 *
	 * @param args
	 *            arguments passed to this application
	 */
	public static void main(String[] args) {

		Application.launch(args);
	}

}
