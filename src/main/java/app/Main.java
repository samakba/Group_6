package app;

import javax.swing.JFrame;

/**
 * The Main class of our application.
 */
public class Main {
    /**
     * Builds and runs the CA architecture of the application.
     * @param args unused arguments
     */
    public static void main(String[] args) {
        // Initialize JavaFX toolkit
        new javafx.embed.swing.JFXPanel(); // This initializes JavaFX
        
        final AppBuilder appBuilder = new AppBuilder();
        final JFrame application = appBuilder
                .addLoginView()
                .addSignupView() 
                .addLoggedInView()
                .addSignupUseCase()
                .addLoginUseCase()
                .addLogoutUseCase()
                .addChangePasswordUseCase()
                .addHeatmapFeature()    // Add your new heatmap feature
                .build();

        application.pack();
        application.setVisible(true);
    }
}