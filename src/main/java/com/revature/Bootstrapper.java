package com.revature;

import com.revature.annotations.URI;
import com.revature.controllers.CatController;
import com.revature.controllers.CatControllerImpl;
import com.revature.controllers.FurnitureController;
import com.revature.controllers.FurnitureControllerImpl;
import com.revature.services.CatService;
import com.revature.services.CatServiceImpl;
import com.revature.servlets.CatServlet;
import com.revature.servlets.FurnitureServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import persistence.SessionFactory;
import util.AnnotationStrategy;
import util.ConnectionPool;
import util.MappingStrategy;
import util.SimpleConnectionPool;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

/**
 * This class is used to inject the dependencies of all servlets, controllers, and services.
 * It is called when the ServletContext is created and initialization begins.
 */
@WebListener
public class Bootstrapper implements ServletContextListener {

    private final static Logger logger = LogManager.getLogger(Bootstrapper.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext context = sce.getServletContext();

        SessionFactory factory = createSessionFactory();
        initializeCatServlet(context, factory);
        initializeFurnitureServlet(context, factory);

        logger.info(context.getServletRegistration("CatServlet").getMappings());
        logger.info(context.getServletRegistration("FurnitureServlet").getMappings());
    }

    private SessionFactory createSessionFactory() {
        Properties props = new Properties();
        try {
            props.load(Bootstrapper.class.getClassLoader().getResourceAsStream("connection.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String url = "jdbc:postgresql://" + props.getProperty("endpoint") + "/postgres";
        String username = props.getProperty("username");
        String password = props.getProperty("password");
        ConnectionPool connectionPool = new SimpleConnectionPool(url, username, password, 2);
        MappingStrategy mappingStrategy = new AnnotationStrategy();

        return new SessionFactory(connectionPool, mappingStrategy);
    }

    private void initializeCatServlet(ServletContext context, SessionFactory factory) {
        CatService catService = new CatServiceImpl(factory);
        CatController catController = new CatControllerImpl(catService);
        ServletRegistration.Dynamic registration = context.addServlet("CatServlet", new CatServlet(catController));
        URI paths = CatServlet.class.getAnnotation(URI.class);
        Arrays.stream(paths.urlPatterns()).forEach(registration::addMapping);
    }

    private void initializeFurnitureServlet(ServletContext context, SessionFactory factory) {
        FurnitureController furnitureController = new FurnitureControllerImpl(null);
        ServletRegistration.Dynamic registration = context.addServlet("FurnitureServlet", new FurnitureServlet(furnitureController));
        URI paths = FurnitureServlet.class.getAnnotation(URI.class);
        Arrays.stream(paths.urlPatterns()).forEach(registration::addMapping);
    }
}
