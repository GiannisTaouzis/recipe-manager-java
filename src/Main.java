import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// Main application entry point
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {
            System.out.println("Could not load FlatLaf theme.");
        }

        SwingUtilities.invokeLater(RecipeManagerGUI::new);
    }
}





class RecipeManagerGUI extends JFrame {

    private static final String SAVE_FILE = "recipes.json";
    private static final String IMAGES_FOLDER = "images";

    private final List<Recipe> recipes = new ArrayList<>();
    private final Map<String, String> mealPlan = new LinkedHashMap<>();

    private final DefaultListModel<Recipe> recipeListModel = new DefaultListModel<>();
    private final JList<Recipe> recipeList = new JList<>(recipeListModel);

    private Recipe editingRecipe = null;
    private boolean darkMode = false;

    private final JTextField nameField = new JTextField();
    private final JComboBox<Cuisine> cuisineBox = new JComboBox<>(Cuisine.values());
    private final JComboBox<RecipeCategory> categoryBox = new JComboBox<>(RecipeCategory.values());
    private final JTextArea ingredientsField = new JTextArea(5, 20);
    private final JTextField prepTimeField = new JTextField();
    private final JTextField caloriesField = new JTextField();
    private final JTextArea instructionsArea = new JTextArea(7, 20);
    private final JTextField tagsField = new JTextField();
    private final JTextField linkField = new JTextField();
    private final JTextField imagePathField = new JTextField();
    private final JLabel imagePreviewLabel = new JLabel("No Image", SwingConstants.CENTER);
    private final JTextField searchField = new JTextField();

    private final JComboBox<String> sortBox = new JComboBox<>(new String[]{
            "Sort by Name", "Sort by Cuisine", "Sort by Prep Time", "Sort by Calories", "Sort by Rating", "Sort by Tags"
    });

    private final JComboBox<String> cuisineFilterBox = new JComboBox<>();
    private final JComboBox<String> categoryFilterBox = new JComboBox<>();
    private final JCheckBox favoritesOnlyBox = new JCheckBox("Favorites only");

    private final JCheckBox favoriteCheckBox = new JCheckBox("Favorite");
    private final JComboBox<Integer> ratingBox = new JComboBox<>(new Integer[]{0, 1, 2, 3, 4, 5});

    private final JButton addButton = new JButton("Add Recipe");
    private final JButton updateButton = new JButton("Update Recipe");
    private final JButton darkModeButton = new JButton("Dark Mode");

    private final JComboBox<MealDay> dayBox = new JComboBox<>(MealDay.values());
    private final JComboBox<MealType> mealTypeBox = new JComboBox<>(MealType.values());
    private final JComboBox<Recipe> mealRecipeBox = new JComboBox<>();
    private final JTextArea weeklyPlanArea = new JTextArea();
    private final JPanel weeklyPlanCardsPanel = new JPanel(new GridLayout(0, 1, 10, 10));

    private final JLabel totalRecipesValue = new JLabel("0");
    private final JLabel favoriteRecipesValue = new JLabel("0");
    private final JLabel averageCaloriesValue = new JLabel("0");
    private final JLabel averageRatingValue = new JLabel("0/5");
    private final JLabel mostCommonCuisineValue = new JLabel("-");
    private final JLabel highestRatedRecipeValue = new JLabel("-");

    private final JLabel timerLabel = new JLabel("00:00", SwingConstants.CENTER);
    private final JTextField timerMinutesField = new JTextField("10");
    private javax.swing.Timer cookingTimer;
    private int timerSecondsRemaining = 0;

    private final JComboBox<Recipe> cookRecipeBox = new JComboBox<>();
    private final JTextArea cookStepArea = new JTextArea();
    private final JLabel cookProgressLabel = new JLabel("Step 0 of 0", SwingConstants.CENTER);
    private final JLabel cookRecipeTitleLabel = new JLabel("Choose a recipe", SwingConstants.CENTER);
    private List<String> currentCookSteps = new ArrayList<>();
    private int currentCookStepIndex = 0;
    private JDialog fullscreenCookDialog;

    private final Deque<String> recentlyViewedRecipes = new ArrayDeque<>();
    private final Deque<String> recentlyCookedRecipes = new ArrayDeque<>();
    private final JTextArea recentActivityArea = new JTextArea();

    public RecipeManagerGUI() {
        setTitle("Recipe Manager");
        setSize(1250, 780);
        setMinimumSize(new Dimension(1000, 650));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(new BorderLayout());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
                dispose();
                System.exit(0);
            }
        });

        setupCuisineFilter();
        setupCategoryFilter();
        buildUI();
        setupListeners();
        setupGlobalKeyboardShortcuts();

        loadData();
        filterRecipes();
        refreshMealRecipeBox();
        refreshCookRecipeBox();
        refreshWeeklyPlanArea();
        refreshStatisticsDashboard();

        updateButton.setEnabled(false);
        setVisible(true);
    }

    private void setupCuisineFilter() {
        cuisineFilterBox.addItem("All cuisines");
        for (Cuisine c : Cuisine.values()) {
            cuisineFilterBox.addItem(c.name());
        }
    }

    private void setupCategoryFilter() {
        categoryFilterBox.addItem("All categories");
        for (RecipeCategory category : RecipeCategory.values()) {
            categoryFilterBox.addItem(category.name());
        }
    }

    // Builds the main application interface
    private void buildUI() {
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setBorder(new EmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("Recipe Manager");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));

        darkModeButton.setPreferredSize(new Dimension(140, 36));

        topPanel.add(title, BorderLayout.WEST);
        topPanel.add(darkModeButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("SansSerif", Font.BOLD, 15));
        tabs.addTab("Recipes", buildRecipesTab());
        tabs.addTab("Add / Edit Recipe", buildFormTab());
        tabs.addTab("Weekly Meal Planner", buildPlannerTab());
        tabs.addTab("Statistics", buildStatisticsTab());
        tabs.addTab("Cook Mode", buildCookModeTab());

        add(tabs, BorderLayout.CENTER);

        darkModeButton.addActionListener(e -> toggleDarkMode());
    }

    // Creates the Recipes tab
    private JPanel buildRecipesTab() {
        JPanel mainPanel = new JPanel(new BorderLayout(12, 12));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        JLabel pageTitle = new JLabel("Recipes");
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchField.setPreferredSize(new Dimension(260, 34));
        sortBox.setPreferredSize(new Dimension(170, 34));
        cuisineFilterBox.setPreferredSize(new Dimension(150, 34));

        filterPanel.add(new JLabel("Search:"));
        filterPanel.add(searchField);
        filterPanel.add(new JLabel("Sort:"));
        filterPanel.add(sortBox);
        filterPanel.add(new JLabel("Cuisine:"));
        filterPanel.add(cuisineFilterBox);
        filterPanel.add(new JLabel("Category:"));
        categoryFilterBox.setPreferredSize(new Dimension(160, 34));
        filterPanel.add(categoryFilterBox);
        filterPanel.add(favoritesOnlyBox);

        topPanel.add(pageTitle, BorderLayout.NORTH);
        topPanel.add(filterPanel, BorderLayout.CENTER);

        recipeList.setCellRenderer(new RecipeCardRenderer());
        recipeList.setFixedCellHeight(145);
        recipeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JButton viewButton = new JButton("View Details");
        JButton editButton = new JButton("Edit Recipe");
        JButton deleteButton = new JButton("Delete Recipe");
        JButton toggleFavoriteButton = new JButton("Toggle Favorite");
        JButton openSelectedLinkButton = new JButton("Open Link");
        JButton randomButton = new JButton("Random Recipe");
        JButton exportButton = new JButton("Export Backup");
        JButton importButton = new JButton("Import Backup");

        Dimension sideButtonSize = new Dimension(150, 34);
        viewButton.setMaximumSize(sideButtonSize);
        editButton.setMaximumSize(sideButtonSize);
        deleteButton.setMaximumSize(sideButtonSize);
        toggleFavoriteButton.setMaximumSize(sideButtonSize);
        openSelectedLinkButton.setMaximumSize(sideButtonSize);
        randomButton.setMaximumSize(sideButtonSize);
        exportButton.setMaximumSize(sideButtonSize);
        importButton.setMaximumSize(sideButtonSize);

        JPanel sideBar = new JPanel();
        sideBar.setLayout(new BoxLayout(sideBar, BoxLayout.Y_AXIS));
        sideBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Actions"),
                new EmptyBorder(10, 10, 10, 10)
        ));
        sideBar.setPreferredSize(new Dimension(190, 0));

        JLabel recipeActionsLabel = new JLabel("Recipe Actions");
        recipeActionsLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        recipeActionsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel toolsLabel = new JLabel("Tools");
        toolsLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        toolsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel backupLabel = new JLabel("Backup");
        backupLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        backupLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        sideBar.add(recipeActionsLabel);
        sideBar.add(Box.createVerticalStrut(8));
        addSidebarButton(sideBar, viewButton);
        addSidebarButton(sideBar, editButton);
        addSidebarButton(sideBar, deleteButton);
        addSidebarButton(sideBar, toggleFavoriteButton);

        sideBar.add(Box.createVerticalStrut(18));
        sideBar.add(new JSeparator());
        sideBar.add(Box.createVerticalStrut(12));
        sideBar.add(toolsLabel);
        sideBar.add(Box.createVerticalStrut(8));
        addSidebarButton(sideBar, openSelectedLinkButton);
        addSidebarButton(sideBar, randomButton);

        sideBar.add(Box.createVerticalGlue());
        sideBar.add(new JSeparator());
        sideBar.add(Box.createVerticalStrut(12));
        sideBar.add(backupLabel);
        sideBar.add(Box.createVerticalStrut(8));
        addSidebarButton(sideBar, exportButton);
        addSidebarButton(sideBar, importButton);

        JPanel contentPanel = new JPanel(new BorderLayout(12, 12));
        contentPanel.add(sideBar, BorderLayout.WEST);
        contentPanel.add(new JScrollPane(recipeList), BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        viewButton.addActionListener(e -> viewRecipe());
        openSelectedLinkButton.addActionListener(e -> openSelectedRecipeLink());
        toggleFavoriteButton.addActionListener(e -> toggleFavorite());
        editButton.addActionListener(e -> editSelectedRecipe());
        deleteButton.addActionListener(e -> deleteSelectedRecipe());
        randomButton.addActionListener(e -> showRandomRecipe());
        exportButton.addActionListener(e -> exportBackup());
        importButton.addActionListener(e -> importBackup());

        return mainPanel;
    }

    private void addSidebarButton(JPanel panel, JButton button) {
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setPreferredSize(new Dimension(150, 34));
        button.setMaximumSize(new Dimension(150, 34));
        panel.add(button);
        panel.add(Box.createVerticalStrut(8));
    }

    // Creates the Add / Edit Recipe tab
    private JPanel buildFormTab() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel pageTitle = new JLabel("Add or Edit Recipe");
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

        ingredientsField.setLineWrap(true);
        ingredientsField.setWrapStyleWord(true);
        instructionsArea.setLineWrap(true);
        instructionsArea.setWrapStyleWord(true);
        instructionsArea.setRows(10);

        JPanel basicInfoPanel = new JPanel(new GridBagLayout());
        basicInfoPanel.setBorder(BorderFactory.createTitledBorder("Basic Information"));

        JPanel recipeDetailsPanel = new JPanel(new GridBagLayout());
        recipeDetailsPanel.setBorder(BorderFactory.createTitledBorder("Recipe Details"));

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createTitledBorder("Content"));

        JPanel linksImagePanel = new JPanel(new BorderLayout(12, 12));
        linksImagePanel.setBorder(BorderFactory.createTitledBorder("Links & Images"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int y = 0;
        addRow(basicInfoPanel, gbc, y++, "Name:", nameField);
        addRow(basicInfoPanel, gbc, y++, "Cuisine:", cuisineBox);
        addRow(basicInfoPanel, gbc, y++, "Category:", categoryBox);

        y = 0;
        addRow(recipeDetailsPanel, gbc, y++, "Preparation Time:", prepTimeField);
        addRow(recipeDetailsPanel, gbc, y++, "Calories:", caloriesField);
        addRow(recipeDetailsPanel, gbc, y++, "Rating:", ratingBox);
        addRow(recipeDetailsPanel, gbc, y++, "Favorite:", favoriteCheckBox);

        y = 0;
        addRow(contentPanel, gbc, y++, "Tags:", tagsField);

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(new JLabel("Ingredients:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.45;
        JScrollPane ingredientsScroll = new JScrollPane(ingredientsField);
        ingredientsScroll.setPreferredSize(new Dimension(420, 120));
        contentPanel.add(ingredientsScroll, gbc);
        y++;

        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        contentPanel.add(new JLabel("Instructions:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        JScrollPane instructionsScroll = new JScrollPane(instructionsArea);
        instructionsScroll.setPreferredSize(new Dimension(420, 250));
        contentPanel.add(instructionsScroll, gbc);

        JPanel leftFormPanel = new JPanel(new BorderLayout(12, 12));
        JPanel topFormPanels = new JPanel(new GridLayout(1, 2, 12, 12));
        topFormPanels.add(basicInfoPanel);
        topFormPanels.add(recipeDetailsPanel);
        leftFormPanel.add(topFormPanels, BorderLayout.NORTH);
        leftFormPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel linkPanel = new JPanel(new GridBagLayout());
        GridBagConstraints linkGbc = new GridBagConstraints();
        linkGbc.insets = new Insets(7, 7, 7, 7);
        linkGbc.fill = GridBagConstraints.HORIZONTAL;
        linkGbc.weightx = 1.0;
        addRow(linkPanel, linkGbc, 0, "Recipe Link:", linkField);
        addRow(linkPanel, linkGbc, 1, "Image Path:", imagePathField);

        imagePreviewLabel.setPreferredSize(new Dimension(280, 210));
        imagePreviewLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imagePreviewLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JButton chooseImageButton = new JButton("Choose / Change Image");
        JButton removeImageButton = new JButton("Remove Image");
        JButton searchImageButton = new JButton("Search Image Online");

        imagePreviewLabel.setTransferHandler(new TransferHandler("text") {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                try {
                    List<File> files = (List<File>) support.getTransferable()
                            .getTransferData(java.awt.datatransfer.DataFlavor.javaFileListFlavor);

                    if (files == null || files.isEmpty()) {
                        return false;
                    }

                    File file = files.get(0);
                    if (!isImageFile(file)) {
                        showError("Please drop an image file.");
                        return false;
                    }

                    String copiedPath = copyImageToAppFolder(file);
                    imagePathField.setText(copiedPath);
                    showImagePreview(copiedPath);
                    return true;
                } catch (Exception e) {
                    showError("Could not import dropped image: " + e.getMessage());
                    return false;
                }
            }
        });

        imagePreviewLabel.setToolTipText("Drag and drop an image here");

        JPanel imageButtons = new JPanel(new GridLayout(3, 1, 8, 8));
        imageButtons.add(chooseImageButton);
        imageButtons.add(removeImageButton);
        imageButtons.add(searchImageButton);

        JPanel imagePanel = new JPanel(new BorderLayout(8, 8));
        imagePanel.add(imagePreviewLabel, BorderLayout.CENTER);
        imagePanel.add(imageButtons, BorderLayout.SOUTH);

        linksImagePanel.add(linkPanel, BorderLayout.NORTH);
        linksImagePanel.add(imagePanel, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(15, 15));
        centerPanel.add(leftFormPanel, BorderLayout.CENTER);
        centerPanel.add(linksImagePanel, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel(new BorderLayout(10, 10));

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        JButton searchWebButton = new JButton("Search Recipe Web");
        JButton openLinkButton = new JButton("Open Link");
        JButton addFromLinkButton = new JButton("Add Recipe from Link");
        leftButtons.add(searchWebButton);
        leftButtons.add(openLinkButton);
        leftButtons.add(addFromLinkButton);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton clearButton = new JButton("Clear Form");
        addButton.setPreferredSize(new Dimension(140, 36));
        updateButton.setPreferredSize(new Dimension(140, 36));
        rightButtons.add(clearButton);
        rightButtons.add(addButton);
        rightButtons.add(updateButton);

        buttonPanel.add(leftButtons, BorderLayout.WEST);
        buttonPanel.add(rightButtons, BorderLayout.EAST);

        mainPanel.add(pageTitle, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> addRecipe());
        updateButton.addActionListener(e -> updateRecipe());
        searchWebButton.addActionListener(e -> searchWeb());
        openLinkButton.addActionListener(e -> openLinkFromField());
        addFromLinkButton.addActionListener(e -> addRecipeFromLink());
        clearButton.addActionListener(e -> clearForm());
        chooseImageButton.addActionListener(e -> chooseImage());
        removeImageButton.addActionListener(e -> removeImage());
        searchImageButton.addActionListener(e -> searchImageOnline());

        return mainPanel;
    }

    // Creates the Weekly Meal Planner tab
    private JPanel buildPlannerTab() {
        JPanel plannerPanel = new JPanel(new BorderLayout(12, 12));
        plannerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel pageTitle = new JLabel("Weekly Meal Planner");
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel selectionCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        selectionCard.setBorder(BorderFactory.createTitledBorder("Assign Recipe to Meal"));

        dayBox.setPreferredSize(new Dimension(150, 34));
        mealTypeBox.setPreferredSize(new Dimension(150, 34));
        mealRecipeBox.setPreferredSize(new Dimension(320, 34));

        selectionCard.add(new JLabel("Day:"));
        selectionCard.add(dayBox);
        selectionCard.add(new JLabel("Meal:"));
        selectionCard.add(mealTypeBox);
        selectionCard.add(new JLabel("Recipe:"));
        selectionCard.add(mealRecipeBox);

        JButton assignMealButton = new JButton("Add to Weekly Plan");
        JButton removeMealButton = new JButton("Remove Meal");
        JButton clearPlanButton = new JButton("Clear Weekly Plan");
        JButton shoppingListButton = new JButton("Generate Shopping List");
        shoppingListButton.setPreferredSize(new Dimension(190, 36));

        selectionCard.add(assignMealButton);
        selectionCard.add(removeMealButton);
        selectionCard.add(clearPlanButton);

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.add(pageTitle, BorderLayout.NORTH);
        topPanel.add(selectionCard, BorderLayout.CENTER);

        weeklyPlanCardsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JScrollPane cardsScroll = new JScrollPane(weeklyPlanCardsPanel);
        cardsScroll.setBorder(BorderFactory.createTitledBorder("Weekly Plan"));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomPanel.add(shoppingListButton);

        plannerPanel.add(topPanel, BorderLayout.NORTH);
        plannerPanel.add(cardsScroll, BorderLayout.CENTER);
        plannerPanel.add(bottomPanel, BorderLayout.SOUTH);

        assignMealButton.addActionListener(e -> assignMeal());
        removeMealButton.addActionListener(e -> removeMeal());
        clearPlanButton.addActionListener(e -> clearMealPlan());
        shoppingListButton.addActionListener(e -> generateShoppingList());

        return plannerPanel;
    }

    private JPanel buildStatisticsTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel pageTitle = new JLabel("Recipe Statistics Dashboard");
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel cardsPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        cardsPanel.add(createStatCard("Total Recipes", totalRecipesValue));
        cardsPanel.add(createStatCard("Favorite Recipes", favoriteRecipesValue));
        cardsPanel.add(createStatCard("Average Calories", averageCaloriesValue));
        cardsPanel.add(createStatCard("Average Rating", averageRatingValue));
        cardsPanel.add(createStatCard("Most Common Cuisine", mostCommonCuisineValue));
        cardsPanel.add(createStatCard("Highest Rated Recipe", highestRatedRecipeValue));

        recentActivityArea.setEditable(false);
        recentActivityArea.setLineWrap(true);
        recentActivityArea.setWrapStyleWord(true);
        recentActivityArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        recentActivityArea.setBorder(BorderFactory.createTitledBorder("Recent Activity"));

        JButton refreshButton = new JButton("Refresh Statistics");
        refreshButton.addActionListener(e -> refreshStatisticsDashboard());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottomPanel.add(refreshButton);

        panel.add(pageTitle, BorderLayout.NORTH);
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                cardsPanel,
                new JScrollPane(recentActivityArea)
        );
        splitPane.setResizeWeight(0.72);

        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(18, 18, 18, 18)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        card.add(titleLabel, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildTimersTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel pageTitle = new JLabel("Cooking Timer");
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel timerCard = new JPanel(new BorderLayout(15, 15));
        timerCard.setBorder(BorderFactory.createTitledBorder("Embedded Timer"));

        timerLabel.setFont(new Font("SansSerif", Font.BOLD, 72));

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        timerMinutesField.setPreferredSize(new Dimension(90, 34));
        JButton startButton = new JButton("Start");
        JButton pauseButton = new JButton("Pause");
        JButton resetButton = new JButton("Reset");

        controlsPanel.add(new JLabel("Minutes:"));
        controlsPanel.add(timerMinutesField);
        controlsPanel.add(startButton);
        controlsPanel.add(pauseButton);
        controlsPanel.add(resetButton);

        JTextArea helpText = new JTextArea("Use this timer while cooking. Enter minutes, press Start, and keep the app open while it counts down.");
        helpText.setEditable(false);
        helpText.setLineWrap(true);
        helpText.setWrapStyleWord(true);
        helpText.setBorder(new EmptyBorder(10, 10, 10, 10));

        timerCard.add(timerLabel, BorderLayout.CENTER);
        timerCard.add(controlsPanel, BorderLayout.SOUTH);

        panel.add(pageTitle, BorderLayout.NORTH);
        panel.add(timerCard, BorderLayout.CENTER);
        panel.add(helpText, BorderLayout.SOUTH);

        startButton.addActionListener(e -> startCookingTimer());
        pauseButton.addActionListener(e -> pauseCookingTimer());
        resetButton.addActionListener(e -> resetCookingTimer());

        return panel;
    }

    private JPanel buildCookModeTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel pageTitle = new JLabel("Cook Mode");
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        cookRecipeBox.setPreferredSize(new Dimension(350, 34));
        JButton loadRecipeButton = new JButton("Load Recipe");
        JButton fullscreenButton = new JButton("Open Fullscreen Cook Mode");

        selectorPanel.add(new JLabel("Recipe:"));
        selectorPanel.add(cookRecipeBox);
        selectorPanel.add(loadRecipeButton);
        selectorPanel.add(fullscreenButton);

        topPanel.add(pageTitle, BorderLayout.NORTH);
        topPanel.add(selectorPanel, BorderLayout.CENTER);

        cookRecipeTitleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        cookProgressLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        cookStepArea.setEditable(false);
        cookStepArea.setLineWrap(true);
        cookStepArea.setWrapStyleWord(true);
        cookStepArea.setFont(new Font("SansSerif", Font.PLAIN, 30));
        cookStepArea.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Current Step"));
        centerPanel.add(cookRecipeTitleLabel, BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(cookStepArea), BorderLayout.CENTER);
        centerPanel.add(cookProgressLabel, BorderLayout.SOUTH);

        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));

        JPanel stepButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        JButton previousStepButton = new JButton("Previous Step");
        JButton nextStepButton = new JButton("Next Step");
        JButton restartStepsButton = new JButton("Restart Steps");

        stepButtons.add(previousStepButton);
        stepButtons.add(restartStepsButton);
        stepButtons.add(nextStepButton);

        JPanel timerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        JButton startTimerButton = new JButton("Start Timer");
        JButton pauseTimerButton = new JButton("Pause Timer");
        JButton resetTimerButton = new JButton("Reset Timer");

        timerPanel.add(new JLabel("Timer minutes:"));
        timerPanel.add(timerMinutesField);
        timerPanel.add(timerLabel);
        timerPanel.add(startTimerButton);
        timerPanel.add(pauseTimerButton);
        timerPanel.add(resetTimerButton);

        bottomPanel.add(stepButtons, BorderLayout.CENTER);
        bottomPanel.add(timerPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        loadRecipeButton.addActionListener(e -> loadSelectedRecipeForCookMode());
        previousStepButton.addActionListener(e -> showPreviousCookStep());
        nextStepButton.addActionListener(e -> showNextCookStep());
        restartStepsButton.addActionListener(e -> restartCookSteps());
        fullscreenButton.addActionListener(e -> openFullscreenCookMode());
        startTimerButton.addActionListener(e -> startCookingTimer());
        pauseTimerButton.addActionListener(e -> pauseCookingTimer());
        resetTimerButton.addActionListener(e -> resetCookingTimer());

        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int y, String label, Component component) {
        gbc.gridx = 0;
        gbc.gridy = y;
        gbc.gridwidth = 1;
        gbc.weighty = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(component, gbc);
    }

    private void setupGlobalKeyboardShortcuts() {
        JRootPane rootPane = getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), "focusSearch");
        actionMap.put("focusSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchField.requestFocusInWindow();
                searchField.selectAll();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newRecipe");
        actionMap.put("newRecipe", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearForm();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "saveRecipes");
        actionMap.put("saveRecipes", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveData();
                JOptionPane.showMessageDialog(RecipeManagerGUI.this, "Recipes saved.");
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_DOWN_MASK), "toggleDarkMode");
        actionMap.put("toggleDarkMode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleDarkMode();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteRecipe");
        actionMap.put("deleteRecipe", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (recipeList.isFocusOwner()) {
                    deleteSelectedRecipe();
                }
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "randomRecipe");
        actionMap.put("randomRecipe", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRandomRecipe();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK), "openRecipeLink");
        actionMap.put("openRecipeLink", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSelectedRecipeLink();
            }
        });
    }

    private void setupListeners() {
        sortBox.addActionListener(e -> filterRecipes());
        cuisineFilterBox.addActionListener(e -> filterRecipes());
        categoryFilterBox.addActionListener(e -> filterRecipes());
        favoritesOnlyBox.addActionListener(e -> filterRecipes());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterRecipes(); }
            public void removeUpdate(DocumentEvent e) { filterRecipes(); }
            public void changedUpdate(DocumentEvent e) { filterRecipes(); }
        });

        imagePathField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { showImagePreview(imagePathField.getText()); }
            public void removeUpdate(DocumentEvent e) { showImagePreview(imagePathField.getText()); }
            public void changedUpdate(DocumentEvent e) { showImagePreview(imagePathField.getText()); }
        });

        recipeList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewRecipe();
                }
            }
        });
    }

    // Adds a new recipe to the application
    private void addRecipe() {
        Recipe recipe = createRecipeFromForm();
        if (recipe == null) return;

        recipes.add(recipe);
        saveData();
        filterRecipes();
        refreshMealRecipeBox();
        refreshStatisticsDashboard();
        clearForm();
    }

    // Updates an existing recipe
    private void updateRecipe() {
        if (editingRecipe == null) {
            showError("No recipe selected for update.");
            return;
        }

        String oldName = editingRecipe.getName();
        Recipe updated = createRecipeFromForm();
        if (updated == null) return;

        editingRecipe.update(
                updated.getName(),
                updated.getCuisine(),
                updated.getIngredients(),
                updated.getPrepTime(),
                updated.getCalories(),
                updated.getInstructions(),
                updated.getTags(),
                updated.getLink(),
                updated.getImagePath(),
                updated.isFavorite(),
                updated.getRating()
        );
        editingRecipe.setCategory(updated.getCategory());

        for (Map.Entry<String, String> entry : mealPlan.entrySet()) {
            if (entry.getValue().equals(oldName)) {
                entry.setValue(updated.getName());
            }
        }

        editingRecipe = null;
        addButton.setEnabled(true);
        updateButton.setEnabled(false);

        saveData();
        filterRecipes();
        refreshMealRecipeBox();
        refreshWeeklyPlanArea();
        refreshStatisticsDashboard();
        clearForm();

        JOptionPane.showMessageDialog(this, "Recipe updated successfully.");
    }

    // Creates a Recipe object using form data
    private Recipe createRecipeFromForm() {
        try {
            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                showError("Name is required.");
                return null;
            }

            Cuisine cuisine = (Cuisine) cuisineBox.getSelectedItem();
            List<String> ingredients = splitCommaText(ingredientsField.getText());

            String prepText = prepTimeField.getText().trim();
            int prepTime = prepText.isEmpty() ? 0 : Integer.parseInt(prepText);

            String caloriesText = caloriesField.getText().trim();
            int calories = caloriesText.isEmpty() ? 0 : Integer.parseInt(caloriesText);

            if (prepTime < 0) {
                showError("Preparation time must be 0 or more.");
                return null;
            }

            if (calories < 0) {
                showError("Calories must be 0 or more.");
                return null;
            }

            String instructions = instructionsArea.getText().trim();
            List<String> tags = splitCommaText(tagsField.getText());
            String link = normalizeLink(linkField.getText().trim());
            String imagePath = imagePathField.getText().trim();
            boolean favorite = favoriteCheckBox.isSelected();
            int rating = (Integer) ratingBox.getSelectedItem();

            Recipe recipe = new Recipe(name, cuisine, ingredients, prepTime, calories, instructions, tags, link, imagePath);
            recipe.setCategory((RecipeCategory) categoryBox.getSelectedItem());
            recipe.setFavorite(favorite);
            recipe.setRating(rating);

            return recipe;

        } catch (NumberFormatException e) {
            showError("Preparation time and calories must be numbers if you fill them.");
            return null;
        }
    }

    private void addRecipeFromLink() {
        try {
            String link = normalizeLink(linkField.getText().trim());

            if (link.isEmpty()) {
                showError("Please enter a recipe link.");
                return;
            }

            String name = nameField.getText().trim();

            if (name.isEmpty()) {
                name = JOptionPane.showInputDialog(this, "Enter recipe name:");

                if (name == null || name.trim().isEmpty()) {
                    showError("Recipe name is required.");
                    return;
                }

                name = name.trim();
            }

            String prepText = prepTimeField.getText().trim();
            int prepTime = prepText.isEmpty() ? 0 : Integer.parseInt(prepText);

            String caloriesText = caloriesField.getText().trim();
            int calories = caloriesText.isEmpty() ? 0 : Integer.parseInt(caloriesText);

            if (prepTime < 0) {
                showError("Preparation time must be 0 or more.");
                return;
            }

            if (calories < 0) {
                showError("Calories must be 0 or more.");
                return;
            }

            Recipe recipe = new Recipe(
                    name,
                    (Cuisine) cuisineBox.getSelectedItem(),
                    splitCommaText(ingredientsField.getText()),
                    prepTime,
                    calories,
                    instructionsArea.getText().trim(),
                    splitCommaText(tagsField.getText()),
                    link,
                    imagePathField.getText().trim()
            );

            recipe.setCategory((RecipeCategory) categoryBox.getSelectedItem());
            recipe.setFavorite(favoriteCheckBox.isSelected());
            recipe.setRating((Integer) ratingBox.getSelectedItem());

            recipes.add(recipe);
            saveData();
            filterRecipes();
            refreshMealRecipeBox();
            refreshStatisticsDashboard();
            clearForm();

            JOptionPane.showMessageDialog(this, "Recipe added from link successfully.");

        } catch (NumberFormatException e) {
            showError("Preparation time and calories must be numbers if you fill them.");
        }
    }

    // Allows the user to choose an image from the computer
    private void chooseImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Recipe Image");
        chooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif", "webp"));

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                File selectedFile = chooser.getSelectedFile();

                if (!isImageFile(selectedFile)) {
                    showError("Please choose a valid image file.");
                    return;
                }

                String copiedPath = copyImageToAppFolder(selectedFile);
                imagePathField.setText(copiedPath);
                showImagePreview(copiedPath);
            } catch (Exception e) {
                showError("Could not choose image: " + e.getMessage());
            }
        }
    }

    private void removeImage() {
        imagePathField.setText("");
        showImagePreview("");
    }

    private boolean isImageFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".gif")
                || name.endsWith(".webp");
    }

    private String copyImageToAppFolder(File selectedFile) throws Exception {
        File folder = new File(IMAGES_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String originalName = selectedFile.getName();
        String safeName = System.currentTimeMillis() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = new File(folder, safeName).toPath();

        Files.copy(selectedFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }

    // Displays image preview in the form panel
    private void showImagePreview(String path) {
        if (path == null || path.isBlank()) {
            imagePreviewLabel.setText("No Image");
            imagePreviewLabel.setIcon(null);
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            imagePreviewLabel.setText("Image not found");
            imagePreviewLabel.setIcon(null);
            return;
        }

        ImageIcon icon = new ImageIcon(path);
        Image image = getScaledImage(icon.getImage(), 260, 180);
        imagePreviewLabel.setText("");
        imagePreviewLabel.setIcon(new ImageIcon(image));
    }

    private ImageIcon createListImageIcon(String path) {
        if (path == null || path.isBlank()) {
            return createPlaceholderIcon(90, 70);
        }

        File file = new File(path);
        if (!file.exists()) {
            return createPlaceholderIcon(90, 70);
        }

        ImageIcon icon = new ImageIcon(path);
        Image image = getScaledImage(icon.getImage(), 90, 70);
        return new ImageIcon(image);
    }

    private Image getScaledImage(Image source, int targetWidth, int targetHeight) {
        int sourceWidth = source.getWidth(null);
        int sourceHeight = source.getHeight(null);

        if (sourceWidth <= 0 || sourceHeight <= 0) {
            return source.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        }

        double scale = Math.min((double) targetWidth / sourceWidth, (double) targetHeight / sourceHeight);
        int newWidth = Math.max(1, (int) (sourceWidth * scale));
        int newHeight = Math.max(1, (int) (sourceHeight * scale));

        return source.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }

    private ImageIcon createPlaceholderIcon(int width, int height) {
        Image image = new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(new Color(230, 230, 230));
        g.fillRect(0, 0, width, height);
        g.setColor(Color.GRAY);
        g.drawRect(0, 0, width - 1, height - 1);
        g.drawString("No Image", 15, height / 2);
        g.dispose();
        return new ImageIcon(image);
    }

    // Opens Google Images search for the recipe name
    private void searchImageOnline() {
        try {
            String query = nameField.getText().trim();
            if (query.isEmpty()) {
                showError("Enter a recipe name first.");
                return;
            }

            String encodedQuery = URLEncoder.encode(query + " recipe image", StandardCharsets.UTF_8);
            String searchURL = "https://www.google.com/search?tbm=isch&q=" + encodedQuery;
            Desktop.getDesktop().browse(new URI(searchURL));
        } catch (Exception e) {
            showError("Could not search image online: " + e.getMessage());
        }
    }

    private void searchWeb() {
        try {
            String query = (nameField.getText() + " " + tagsField.getText() + " recipe").trim();

            if (query.isEmpty() || query.equals("recipe")) {
                showError("Enter recipe name or tags first.");
                return;
            }

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String searchURL = "https://www.google.com/search?q=" + encodedQuery;
            Desktop.getDesktop().browse(new URI(searchURL));

        } catch (Exception e) {
            showError("Could not search web: " + e.getMessage());
        }
    }

    private void openLinkFromField() {
        String link = normalizeLink(linkField.getText().trim());

        if (link.isEmpty()) {
            showError("Please enter a link first.");
            return;
        }

        openLink(link);
    }

    private void openSelectedRecipeLink() {
        Recipe recipe = recipeList.getSelectedValue();

        if (recipe == null) {
            showError("Select a recipe first.");
            return;
        }

        if (recipe.getLink() == null || recipe.getLink().isBlank()) {
            showError("This recipe has no link.");
            return;
        }

        openLink(recipe.getLink());
    }

    private void openLink(String link) {
        try {
            link = normalizeLink(link);
            Desktop.getDesktop().browse(new URI(link));
        } catch (Exception e) {
            showError("Could not open link: " + e.getMessage());
        }
    }

    // Opens a detailed recipe window
    private void viewRecipe() {
        Recipe recipe = recipeList.getSelectedValue();

        if (recipe == null) {
            showError("Select a recipe first.");
            return;
        }

        addRecentItem(recentlyViewedRecipes, recipe.getName());
        refreshStatisticsDashboard();

        JDialog dialog = new JDialog(this, recipe.getName(), true);
        dialog.setSize(820, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(12, 12));

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(280, 220));
        imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        if (recipe.getImagePath() != null && !recipe.getImagePath().isBlank() && new File(recipe.getImagePath()).exists()) {
            ImageIcon icon = new ImageIcon(recipe.getImagePath());
            Image image = getScaledImage(icon.getImage(), 280, 220);
            imageLabel.setIcon(new ImageIcon(image));
        } else {
            imageLabel.setText("No Image");
            imageLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        }

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel((recipe.isFavorite() ? "★ " : "") + recipe.getName());
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel metaLabel = new JLabel(recipe.getCuisine() + " • " + recipe.getCategory() + " • " + recipe.getPrepTime() + " mins • " + recipe.getCalories() + " kcal • Rating: " + recipe.getRating() + "/5");
        metaLabel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagsLabel = new JLabel(recipe.getTags().isEmpty() ? "Tags: -" : "Tags: " + String.join(", ", recipe.getTags()));
        tagsLabel.setFont(new Font("SansSerif", Font.ITALIC, 14));
        tagsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea ingredientsArea = new JTextArea(String.join(System.lineSeparator(), recipe.getIngredients()));
        ingredientsArea.setEditable(false);
        ingredientsArea.setLineWrap(true);
        ingredientsArea.setWrapStyleWord(true);
        ingredientsArea.setBorder(BorderFactory.createTitledBorder("Ingredients"));
        ingredientsArea.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea instructionsTextArea = new JTextArea(recipe.getInstructions());
        instructionsTextArea.setEditable(false);
        instructionsTextArea.setLineWrap(true);
        instructionsTextArea.setWrapStyleWord(true);
        instructionsTextArea.setBorder(BorderFactory.createTitledBorder("Instructions"));

        infoPanel.add(titleLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(metaLabel);
        infoPanel.add(Box.createVerticalStrut(8));
        infoPanel.add(tagsLabel);
        infoPanel.add(Box.createVerticalStrut(12));
        infoPanel.add(new JScrollPane(ingredientsArea));

        JPanel topContent = new JPanel(new BorderLayout(15, 15));
        topContent.add(imageLabel, BorderLayout.WEST);
        topContent.add(infoPanel, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton openButton = new JButton("Open Link");
        JButton closeButton = new JButton("Close");

        openButton.addActionListener(e -> {
            if (recipe.getLink() == null || recipe.getLink().isBlank()) {
                showError("This recipe has no link.");
            } else {
                openLink(recipe.getLink());
            }
        });

        closeButton.addActionListener(e -> dialog.dispose());

        buttons.add(openButton);
        buttons.add(closeButton);

        mainPanel.add(topContent, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(instructionsTextArea), BorderLayout.CENTER);

        dialog.add(mainPanel, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void editSelectedRecipe() {
        Recipe recipe = recipeList.getSelectedValue();

        if (recipe == null) {
            showError("Select a recipe first.");
            return;
        }

        editingRecipe = recipe;

        nameField.setText(recipe.getName());
        cuisineBox.setSelectedItem(recipe.getCuisine());
        categoryBox.setSelectedItem(recipe.getCategory());
        ingredientsField.setText(String.join(", ", recipe.getIngredients()));
        prepTimeField.setText(String.valueOf(recipe.getPrepTime()));
        caloriesField.setText(String.valueOf(recipe.getCalories()));
        instructionsArea.setText(recipe.getInstructions());
        tagsField.setText(String.join(", ", recipe.getTags()));
        linkField.setText(recipe.getLink());
        imagePathField.setText(recipe.getImagePath());
        showImagePreview(recipe.getImagePath());
        favoriteCheckBox.setSelected(recipe.isFavorite());
        ratingBox.setSelectedItem(recipe.getRating());

        addButton.setEnabled(false);
        updateButton.setEnabled(true);
    }

    private void deleteSelectedRecipe() {
        Recipe recipe = recipeList.getSelectedValue();

        if (recipe == null) {
            showError("Select a recipe first.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete recipe: " + recipe.getName() + "?",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            recipes.remove(recipe);
            mealPlan.entrySet().removeIf(entry -> entry.getValue().equals(recipe.getName()));

            if (editingRecipe == recipe) {
                editingRecipe = null;
                addButton.setEnabled(true);
                updateButton.setEnabled(false);
                clearForm();
            }

            saveData();
            filterRecipes();
            refreshMealRecipeBox();
            refreshWeeklyPlanArea();
            refreshStatisticsDashboard();
        }
    }

    private void toggleFavorite() {
        Recipe recipe = recipeList.getSelectedValue();

        if (recipe == null) {
            showError("Select a recipe first.");
            return;
        }

        recipe.setFavorite(!recipe.isFavorite());
        saveData();
        filterRecipes();
        refreshStatisticsDashboard();
    }

    private void showRandomRecipe() {
        List<Recipe> visibleRecipes = Collections.list(recipeListModel.elements());

        if (visibleRecipes.isEmpty()) {
            showError("No recipes available.");
            return;
        }

        Recipe randomRecipe = visibleRecipes.get(new Random().nextInt(visibleRecipes.size()));
        recipeList.setSelectedValue(randomRecipe, true);
        JOptionPane.showMessageDialog(this, randomRecipe.getFullDetails(), "Random Recipe", JOptionPane.INFORMATION_MESSAGE);
    }

    private void assignMeal() {
        Recipe recipe = (Recipe) mealRecipeBox.getSelectedItem();

        if (recipe == null) {
            showError("No recipe selected for meal plan.");
            return;
        }

        String key = getMealKey();
        mealPlan.put(key, recipe.getName());

        saveData();
        refreshWeeklyPlanArea();
    }

    private void removeMeal() {
        String key = getMealKey();

        if (mealPlan.remove(key) == null) {
            showError("No meal exists in this slot.");
            return;
        }

        saveData();
        refreshWeeklyPlanArea();
    }

    private void clearMealPlan() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Clear the whole weekly meal plan?",
                "Confirm",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            mealPlan.clear();
            saveData();
            refreshWeeklyPlanArea();
        }
    }

    // Generates a shopping list using recipes from the weekly plan
    private void generateShoppingList() {
        if (mealPlan.isEmpty()) {
            showError("The weekly meal plan is empty.");
            return;
        }

        Map<String, Recipe> recipeByName = new LinkedHashMap<>();
        for (Recipe recipe : recipes) {
            recipeByName.put(recipe.getName(), recipe);
        }

        String nl = System.lineSeparator();
        StringBuilder groupedList = new StringBuilder();
        Set<String> combinedIngredients = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        groupedList.append("SHOPPING LIST BY RECIPE").append(nl).append(nl);

        boolean foundRecipe = false;

        for (MealDay day : MealDay.values()) {
            for (MealType meal : MealType.values()) {
                String key = day + "_" + meal;
                String recipeName = mealPlan.get(key);

                if (recipeName == null || recipeName.isBlank()) continue;

                Recipe recipe = recipeByName.get(recipeName);
                if (recipe == null) continue;

                foundRecipe = true;
                groupedList.append(day)
                        .append(" - ")
                        .append(meal)
                        .append(": ")
                        .append(recipe.getName())
                        .append(nl);

                if (recipe.getIngredients().isEmpty()) {
                    groupedList.append("  - No ingredients added").append(nl);
                } else {
                    for (String ingredient : recipe.getIngredients()) {
                        groupedList.append("  - ").append(ingredient).append(nl);
                        combinedIngredients.add(ingredient);
                    }
                }

                groupedList.append(nl);
            }
        }

        if (!foundRecipe) {
            showError("No valid recipes were found in the weekly plan.");
            return;
        }

        StringBuilder combinedList = new StringBuilder();
        combinedList.append("COMBINED SIMPLE LIST").append(nl).append(nl);

        if (combinedIngredients.isEmpty()) {
            combinedList.append("No ingredients added.").append(nl);
        } else {
            for (String ingredient : combinedIngredients) {
                combinedList.append("- ").append(ingredient).append(nl);
            }
        }

        JDialog dialog = new JDialog(this, "Shopping List", true);
        dialog.setSize(720, 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JTextArea shoppingArea = new JTextArea(groupedList.toString() + nl + combinedList.toString());
        shoppingArea.setEditable(false);
        shoppingArea.setLineWrap(true);
        shoppingArea.setWrapStyleWord(true);
        shoppingArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton copyButton = new JButton("Copy");
        JButton saveButton = new JButton("Save as TXT");
        JButton closeButton = new JButton("Close");

        copyButton.addActionListener(e -> {
            shoppingArea.selectAll();
            shoppingArea.copy();
            shoppingArea.select(0, 0);
            JOptionPane.showMessageDialog(dialog, "Shopping list copied.");
        });

        saveButton.addActionListener(e -> saveShoppingListToFile(shoppingArea.getText()));
        closeButton.addActionListener(e -> dialog.dispose());

        buttons.add(copyButton);
        buttons.add(saveButton);
        buttons.add(closeButton);

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new BoxLayout(checkboxPanel, BoxLayout.Y_AXIS));
        checkboxPanel.setBorder(BorderFactory.createTitledBorder("Check Off Items"));

        if (combinedIngredients.isEmpty()) {
            checkboxPanel.add(new JLabel("No ingredients available."));
        } else {
            for (String ingredient : combinedIngredients) {
                JCheckBox itemBox = new JCheckBox(ingredient);
                checkboxPanel.add(itemBox);
            }
        }

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                new JScrollPane(shoppingArea),
                new JScrollPane(checkboxPanel)
        );
        splitPane.setResizeWeight(0.65);

        dialog.add(splitPane, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // Saves the generated shopping list as a text file
    private void saveShoppingListToFile(String text) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Shopping List");
        chooser.setSelectedFile(new File("shopping_list.txt"));

        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), text, StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "Shopping list saved successfully.");
            } catch (Exception e) {
                showError("Could not save shopping list: " + e.getMessage());
            }
        }
    }

    private String getMealKey() {
        return dayBox.getSelectedItem() + "_" + mealTypeBox.getSelectedItem();
    }

    private void refreshWeeklyPlanArea() {
        weeklyPlanCardsPanel.removeAll();

        for (MealDay day : MealDay.values()) {
            JPanel dayCard = new JPanel(new GridLayout(4, 1, 5, 5));
            dayCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(day.toString()),
                    new EmptyBorder(8, 8, 8, 8)
            ));

            JLabel dayTitle = new JLabel(day.toString());
            dayTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
            dayCard.add(dayTitle);

            for (MealType meal : MealType.values()) {
                String key = day + "_" + meal;
                String recipeName = mealPlan.getOrDefault(key, "-");
                JLabel mealLabel = new JLabel(meal + ": " + recipeName);
                mealLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
                dayCard.add(mealLabel);
            }

            weeklyPlanCardsPanel.add(dayCard);
        }

        weeklyPlanCardsPanel.revalidate();
        weeklyPlanCardsPanel.repaint();
    }

    private void refreshStatisticsDashboard() {
        totalRecipesValue.setText(String.valueOf(recipes.size()));

        long favoriteCount = recipes.stream().filter(Recipe::isFavorite).count();
        favoriteRecipesValue.setText(String.valueOf(favoriteCount));

        double avgCalories = recipes.stream()
                .mapToInt(Recipe::getCalories)
                .average()
                .orElse(0);
        averageCaloriesValue.setText(String.format("%.0f kcal", avgCalories));

        double avgRating = recipes.stream()
                .mapToInt(Recipe::getRating)
                .average()
                .orElse(0);
        averageRatingValue.setText(String.format("%.1f/5", avgRating));

        String mostCommonCuisine = recipes.stream()
                .collect(Collectors.groupingBy(r -> r.getCuisine().name(), Collectors.counting()))
                .entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
        mostCommonCuisineValue.setText(mostCommonCuisine);

        String highestRatedRecipe = recipes.stream()
                .max(Comparator.comparingInt(Recipe::getRating))
                .map(Recipe::getName)
                .orElse("-");
        highestRatedRecipeValue.setText(highestRatedRecipe);

        recentActivityArea.setText(buildRecentActivityText());
    }

    private void addRecentItem(Deque<String> list, String recipeName) {
        if (recipeName == null || recipeName.isBlank()) {
            return;
        }

        list.remove(recipeName);
        list.addFirst(recipeName);

        while (list.size() > 5) {
            list.removeLast();
        }
    }

    private String buildRecentActivityText() {
        StringBuilder sb = new StringBuilder();
        String nl = System.lineSeparator();

        sb.append("Recently Viewed:").append(nl);
        if (recentlyViewedRecipes.isEmpty()) {
            sb.append("- None yet").append(nl);
        } else {
            for (String recipeName : recentlyViewedRecipes) {
                sb.append("- ").append(recipeName).append(nl);
            }
        }

        sb.append(nl).append("Recently Cooked:").append(nl);
        if (recentlyCookedRecipes.isEmpty()) {
            sb.append("- None yet").append(nl);
        } else {
            for (String recipeName : recentlyCookedRecipes) {
                sb.append("- ").append(recipeName).append(nl);
            }
        }

        return sb.toString();
    }

    private void startCookingTimer() {
        try {
            if (timerSecondsRemaining <= 0) {
                int minutes = Integer.parseInt(timerMinutesField.getText().trim());

                if (minutes <= 0) {
                    showError("Timer minutes must be greater than 0.");
                    return;
                }

                timerSecondsRemaining = minutes * 60;
            }

            if (cookingTimer != null && cookingTimer.isRunning()) {
                return;
            }

            cookingTimer = new javax.swing.Timer(1000, e -> {
                timerSecondsRemaining--;
                updateTimerLabel();

                if (timerSecondsRemaining <= 0) {
                    cookingTimer.stop();
                    timerSecondsRemaining = 0;
                    updateTimerLabel();
                    JOptionPane.showMessageDialog(this, "Timer finished!");
                }
            });

            cookingTimer.start();
            updateTimerLabel();
        } catch (NumberFormatException e) {
            showError("Please enter a valid number of minutes.");
        }
    }

    private void pauseCookingTimer() {
        if (cookingTimer != null) {
            cookingTimer.stop();
        }
    }

    private void resetCookingTimer() {
        if (cookingTimer != null) {
            cookingTimer.stop();
        }

        timerSecondsRemaining = 0;
        updateTimerLabel();
    }

    private void updateTimerLabel() {
        int minutes = timerSecondsRemaining / 60;
        int seconds = timerSecondsRemaining % 60;
        timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }

    private void refreshMealRecipeBox() {
        Recipe selected = (Recipe) mealRecipeBox.getSelectedItem();

        mealRecipeBox.removeAllItems();

        for (Recipe recipe : recipes) {
            mealRecipeBox.addItem(recipe);
        }

        if (selected != null) {
            mealRecipeBox.setSelectedItem(selected);
        }
    }

    private void refreshCookRecipeBox() {
        Recipe selected = (Recipe) cookRecipeBox.getSelectedItem();

        cookRecipeBox.removeAllItems();

        for (Recipe recipe : recipes) {
            cookRecipeBox.addItem(recipe);
        }

        if (selected != null) {
            cookRecipeBox.setSelectedItem(selected);
        }
    }

    private void loadSelectedRecipeForCookMode() {
        Recipe recipe = (Recipe) cookRecipeBox.getSelectedItem();

        if (recipe == null) {
            showError("Select a recipe first.");
            return;
        }

        currentCookSteps = extractInstructionSteps(recipe.getInstructions());
        currentCookStepIndex = 0;
        cookRecipeTitleLabel.setText(recipe.getName());
        addRecentItem(recentlyCookedRecipes, recipe.getName());
        refreshStatisticsDashboard();
        updateCookStepDisplay();
    }

    private List<String> extractInstructionSteps(String instructions) {
        if (instructions == null || instructions.isBlank()) {
            return List.of("No instructions available for this recipe.");
        }

        List<String> steps = Arrays.stream(instructions.split("\r?\n|(?<=[.!?])\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        return steps.isEmpty() ? List.of(instructions.trim()) : steps;
    }

    private void updateCookStepDisplay() {
        if (currentCookSteps == null || currentCookSteps.isEmpty()) {
            cookStepArea.setText("Load a recipe to begin cooking.");
            cookProgressLabel.setText("Step 0 of 0");
            return;
        }

        cookStepArea.setText(currentCookSteps.get(currentCookStepIndex));
        cookStepArea.setCaretPosition(0);
        cookProgressLabel.setText("Step " + (currentCookStepIndex + 1) + " of " + currentCookSteps.size());
    }

    private void setupCookModeKeyBindings(JComponent component) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "nextCookStep");
        actionMap.put("nextCookStep", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNextCookStep();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "previousCookStep");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previousCookStep");
        actionMap.put("previousCookStep", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPreviousCookStep();
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitCookMode");
        actionMap.put("exitCookMode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeFullscreenCookMode();
            }
        });
    }

    private void showNextCookStep() {
        if (currentCookSteps == null || currentCookSteps.isEmpty()) {
            showError("Load a recipe first.");
            return;
        }

        if (currentCookStepIndex < currentCookSteps.size() - 1) {
            currentCookStepIndex++;
            updateCookStepDisplay();
        } else {
            JOptionPane.showMessageDialog(this, "You reached the final step.");
        }
    }

    private void showPreviousCookStep() {
        if (currentCookSteps == null || currentCookSteps.isEmpty()) {
            showError("Load a recipe first.");
            return;
        }

        if (currentCookStepIndex > 0) {
            currentCookStepIndex--;
            updateCookStepDisplay();
        }
    }

    private void restartCookSteps() {
        if (currentCookSteps == null || currentCookSteps.isEmpty()) {
            showError("Load a recipe first.");
            return;
        }

        currentCookStepIndex = 0;
        updateCookStepDisplay();
    }

    private void openFullscreenCookMode() {
        if (currentCookSteps == null || currentCookSteps.isEmpty()) {
            loadSelectedRecipeForCookMode();
        }

        if (currentCookSteps == null || currentCookSteps.isEmpty()) {
            return;
        }

        fullscreenCookDialog = new JDialog(this, "Cook Mode", false);
        fullscreenCookDialog.setUndecorated(true);
        fullscreenCookDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        fullscreenCookDialog.setLayout(new BorderLayout(20, 20));

        JPanel content = new JPanel(new BorderLayout(20, 20));
        content.setBorder(new EmptyBorder(30, 40, 30, 40));

        JLabel title = new JLabel(cookRecipeTitleLabel.getText(), SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 34));

        JLabel progress = new JLabel("Step " + (currentCookStepIndex + 1) + " of " + currentCookSteps.size(), SwingConstants.CENTER);
        progress.setFont(new Font("SansSerif", Font.BOLD, 20));

        JTextArea stepArea = new JTextArea(currentCookSteps.get(currentCookStepIndex));
        stepArea.setEditable(false);
        stepArea.setLineWrap(true);
        stepArea.setWrapStyleWord(true);
        stepArea.setFont(new Font("SansSerif", Font.PLAIN, 42));
        stepArea.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        JButton previousButton = new JButton("Previous");
        JButton nextButton = new JButton("Next");
        JButton exitButton = new JButton("Exit Cook Mode");

        previousButton.addActionListener(e -> {
            showPreviousCookStep();
            stepArea.setText(currentCookSteps.get(currentCookStepIndex));
            progress.setText("Step " + (currentCookStepIndex + 1) + " of " + currentCookSteps.size());
        });

        nextButton.addActionListener(e -> {
            showNextCookStep();
            stepArea.setText(currentCookSteps.get(currentCookStepIndex));
            progress.setText("Step " + (currentCookStepIndex + 1) + " of " + currentCookSteps.size());
        });

        exitButton.addActionListener(e -> closeFullscreenCookMode());

        buttons.add(previousButton);
        buttons.add(nextButton);
        buttons.add(exitButton);

        content.add(title, BorderLayout.NORTH);
        content.add(new JScrollPane(stepArea), BorderLayout.CENTER);
        content.add(progress, BorderLayout.SOUTH);

        fullscreenCookDialog.add(content, BorderLayout.CENTER);
        fullscreenCookDialog.add(buttons, BorderLayout.SOUTH);

        JRootPane rootPane = fullscreenCookDialog.getRootPane();
        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "nextStep");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "nextStep");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextStep");
        actionMap.put("nextStep", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNextCookStep();
                stepArea.setText(currentCookSteps.get(currentCookStepIndex));
                progress.setText("Step " + (currentCookStepIndex + 1) + " of " + currentCookSteps.size());
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "previousStep");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "previousStep");
        actionMap.put("previousStep", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPreviousCookStep();
                stepArea.setText(currentCookSteps.get(currentCookStepIndex));
                progress.setText("Step " + (currentCookStepIndex + 1) + " of " + currentCookSteps.size());
            }
        });

        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exitCookMode");
        actionMap.put("exitCookMode", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeFullscreenCookMode();
            }
        });

        Rectangle screenBounds = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();

        fullscreenCookDialog.setBounds(screenBounds);
        fullscreenCookDialog.setAlwaysOnTop(true);
        fullscreenCookDialog.setVisible(true);
    }

    private void closeFullscreenCookMode() {
        if (fullscreenCookDialog != null) {
            fullscreenCookDialog.setAlwaysOnTop(false);
            fullscreenCookDialog.setVisible(false);
            fullscreenCookDialog.dispose();
            fullscreenCookDialog = null;
        }

        setVisible(true);
        toFront();
        requestFocus();
    }

    private void clearForm() {
        nameField.setText("");
        ingredientsField.setText("");
        prepTimeField.setText("");
        caloriesField.setText("");
        instructionsArea.setText("");
        tagsField.setText("");
        linkField.setText("");
        imagePathField.setText("");
        showImagePreview("");
        cuisineBox.setSelectedIndex(0);
        categoryBox.setSelectedIndex(1);
        favoriteCheckBox.setSelected(false);
        ratingBox.setSelectedIndex(0);

        editingRecipe = null;
        addButton.setEnabled(true);
        updateButton.setEnabled(false);
    }

    // Filters and sorts recipes based on search and selected filters
    private void filterRecipes() {
        String searchText = searchField.getText().trim().toLowerCase();
        String selectedCuisine = (String) cuisineFilterBox.getSelectedItem();
        String selectedCategory = (String) categoryFilterBox.getSelectedItem();
        boolean favoritesOnly = favoritesOnlyBox.isSelected();

        List<String> searchTerms = Arrays.stream(searchText.split("[,\s]+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<Recipe> filtered = recipes.stream()
                .filter(r -> matchesSearchTerms(r, searchTerms))
                .filter(r -> selectedCuisine == null
                        || selectedCuisine.equals("All cuisines")
                        || r.getCuisine().name().equals(selectedCuisine))
                .filter(r -> selectedCategory == null
                        || selectedCategory.equals("All categories")
                        || r.getCategory().name().equals(selectedCategory))
                .filter(r -> !favoritesOnly || r.isFavorite())
                .collect(Collectors.toList());

        Comparator<Recipe> sortBy = switch (sortBox.getSelectedIndex()) {
            case 0 -> Comparator.comparing(Recipe::getName, String.CASE_INSENSITIVE_ORDER);
            case 1 -> Comparator.comparing(r -> r.getCuisine().name());
            case 2 -> Comparator.comparingInt(Recipe::getPrepTime);
            case 3 -> Comparator.comparingInt(Recipe::getCalories);
            case 4 -> Comparator.comparingInt(Recipe::getRating).reversed();
            case 5 -> Comparator.comparing(r -> r.getTags().isEmpty() ? "" : r.getTags().get(0), String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(Recipe::getName, String.CASE_INSENSITIVE_ORDER);
        };

        filtered.sort(Comparator.comparing(Recipe::isFavorite).reversed().thenComparing(sortBy));

        recipeListModel.clear();

        for (Recipe recipe : filtered) {
            recipeListModel.addElement(recipe);
        }
    }

    private boolean matchesSearchTerms(Recipe recipe, List<String> searchTerms) {
        if (searchTerms.isEmpty()) {
            return true;
        }

        String searchableText = String.join(" ",
                recipe.getName(),
                recipe.getCuisine().name(),
                recipe.getCategory().name(),
                String.join(" ", recipe.getIngredients()),
                String.join(" ", recipe.getTags())
        ).toLowerCase();

        // Multi-tag / multi-term search: every typed term must exist somewhere in the recipe.
        return searchTerms.stream().allMatch(searchableText::contains);
    }

    // Switches between light and dark FlatLaf themes
    private void toggleDarkMode() {
        try {
            darkMode = !darkMode;

            if (darkMode) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                darkModeButton.setText("Light Mode");
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                darkModeButton.setText("Dark Mode");
            }

            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            showError("Could not switch theme: " + e.getMessage());
        }
    }

    private void exportBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Recipes Backup");
        chooser.setSelectedFile(new File("recipes_backup.json"));

        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), appToJson(), StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(this, "Backup exported successfully.");
            } catch (Exception e) {
                showError("Could not export backup: " + e.getMessage());
            }
        }
    }

    private void importBackup() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Import Recipes Backup");

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                String json = Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8);
                loadFromJsonText(json, true);
                saveData();
                filterRecipes();
                refreshMealRecipeBox();
                refreshWeeklyPlanArea();
                refreshStatisticsDashboard();
                JOptionPane.showMessageDialog(this, "Backup imported successfully.");
            } catch (Exception e) {
                showError("Could not import backup: " + e.getMessage());
            }
        }
    }

    private void saveData() {
        try {
            Files.writeString(new File(SAVE_FILE).toPath(), appToJson(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not save data: " + e.getMessage());
        }
    }

    private void loadData() {
        File file = new File(SAVE_FILE);

        if (!file.exists()) {
            return;
        }

        try {
            String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            loadFromJsonText(json, false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not load data: " + e.getMessage());
        }
    }

    private void loadFromJsonText(String json, boolean append) {
        Object parsed = new SimpleJsonParser(json).parse();

        if (!append) {
            recipes.clear();
            mealPlan.clear();
        }

        if (parsed instanceof List<?> oldList) {
            recipes.addAll(recipesFromList(oldList));
            return;
        }

        if (parsed instanceof Map<?, ?> map) {
            Object recipesObj = map.get("recipes");
            Object mealPlanObj = map.get("mealPlan");

            if (recipesObj instanceof List<?> list) {
                recipes.addAll(recipesFromList(list));
            }

            if (mealPlanObj instanceof Map<?, ?> planMap) {
                for (Map.Entry<?, ?> entry : planMap.entrySet()) {
                    mealPlan.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
        }
    }

    private List<Recipe> recipesFromList(List<?> list) {
        List<Recipe> loaded = new ArrayList<>();

        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) continue;

            String name = stringValue(map.get("name"), "Imported Recipe");
            Cuisine cuisine = cuisineValue(map.get("cuisine"));
            List<String> ingredients = stringListValue(map.get("ingredients"));
            int prepTime = intValue(map.get("prepTime"), 30);
            int calories = intValue(map.get("calories"), 0);
            String instructions = stringValue(map.get("instructions"), "");
            List<String> tags = stringListValue(map.get("tags"));
            String link = stringValue(map.get("link"), "");
            RecipeCategory category = categoryValue(map.get("category"));
            String imagePath = stringValue(map.get("imagePath"), "");
            boolean favorite = booleanValue(map.get("favorite"));
            int rating = intValue(map.get("rating"), 0);

            Recipe recipe = new Recipe(name, cuisine, ingredients, prepTime, calories, instructions, tags, link, imagePath);
            recipe.setCategory(category);
            recipe.setFavorite(favorite);
            recipe.setRating(rating);

            loaded.add(recipe);
        }

        return loaded;
    }

    private String appToJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"recipes\": ").append(recipesToJson()).append(",\n");
        sb.append("  \"mealPlan\": ").append(mealPlanToJson()).append("\n");
        sb.append("}");
        return sb.toString();
    }

    private String recipesToJson() {
        StringBuilder sb = new StringBuilder();
        String nl = System.lineSeparator();

        sb.append("[").append(nl);

        for (int i = 0; i < recipes.size(); i++) {
            Recipe r = recipes.get(i);

            sb.append("    {").append(nl);
            sb.append("      ").append(jsonString("name")).append(": ").append(jsonString(r.getName())).append(",").append(nl);
            sb.append("      ").append(jsonString("cuisine")).append(": ").append(jsonString(r.getCuisine().name())).append(",").append(nl);
            sb.append("      ").append(jsonString("category")).append(": ").append(jsonString(r.getCategory().name())).append(",").append(nl);
            sb.append("      ").append(jsonString("ingredients")).append(": ").append(jsonArray(r.getIngredients())).append(",").append(nl);
            sb.append("      ").append(jsonString("prepTime")).append(": ").append(r.getPrepTime()).append(",").append(nl);
            sb.append("      ").append(jsonString("calories")).append(": ").append(r.getCalories()).append(",").append(nl);
            sb.append("      ").append(jsonString("instructions")).append(": ").append(jsonString(r.getInstructions())).append(",").append(nl);
            sb.append("      ").append(jsonString("tags")).append(": ").append(jsonArray(r.getTags())).append(",").append(nl);
            sb.append("      ").append(jsonString("link")).append(": ").append(jsonString(r.getLink())).append(",").append(nl);
            sb.append("      ").append(jsonString("imagePath")).append(": ").append(jsonString(r.getImagePath())).append(",").append(nl);
            sb.append("      ").append(jsonString("favorite")).append(": ").append(r.isFavorite()).append(",").append(nl);
            sb.append("      ").append(jsonString("rating")).append(": ").append(r.getRating()).append(nl);
            sb.append("    }");

            if (i < recipes.size() - 1) {
                sb.append(",");
            }

            sb.append(nl);
        }

        sb.append("  ]");
        return sb.toString();
    }


    private String mealPlanToJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        int i = 0;
        for (Map.Entry<String, String> entry : mealPlan.entrySet()) {
            sb.append("\n    ");
            sb.append(jsonString(entry.getKey()));
            sb.append(": ");
            sb.append(jsonString(entry.getValue()));

            if (i < mealPlan.size() - 1) {
                sb.append(",");
            }

            i++;
        }

        if (!mealPlan.isEmpty()) {
            sb.append("\n  ");
        }

        sb.append("}");
        return sb.toString();
    }

    private String jsonString(String value) {
        if (value == null) return "\"\"";

        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + "\"";
    }

    private String jsonArray(List<String> values) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (int i = 0; i < values.size(); i++) {
            sb.append(jsonString(values.get(i)));

            if (i < values.size() - 1) {
                sb.append(", ");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    private String normalizeLink(String link) {
        if (link == null || link.isBlank()) return "";

        link = link.trim();

        if (!link.startsWith("http://") && !link.startsWith("https://")) {
            link = "https://" + link;
        }

        return link;
    }

    private List<String> splitCommaText(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        // Supports both comma-separated and line-by-line input.
        return Arrays.stream(text.split("[,\n]"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private Cuisine cuisineValue(Object value) {
        try {
            return Cuisine.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return Cuisine.GREEK;
        }
    }

    private RecipeCategory categoryValue(Object value) {
        try {
            return RecipeCategory.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return RecipeCategory.MAIN_COURSE;
        }
    }

    private String stringValue(Object value, String defaultValue) {
        return value == null ? defaultValue : String.valueOf(value);
    }

    private int intValue(Object value, int defaultValue) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean booleanValue(Object value) {
        if (value instanceof Boolean b) return b;
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private List<String> stringListValue(Object value) {
        List<String> result = new ArrayList<>();

        if (value instanceof List<?> list) {
            for (Object item : list) {
                result.add(String.valueOf(item));
            }
        }

        return result;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private class RecipeCardRenderer extends JPanel implements ListCellRenderer<Recipe> {
        private final JLabel imageLabel = new JLabel();
        private final JLabel titleLabel = new JLabel();
        private final JLabel cuisineLabel = new JLabel();
        private final JLabel statsLabel = new JLabel();
        private final JLabel starsLabel = new JLabel();
        private final JLabel tagsLabel = new JLabel();
        private final JLabel favoriteIconLabel = new JLabel();

        public RecipeCardRenderer() {
            setLayout(new BorderLayout(14, 8));
            setBorder(new EmptyBorder(12, 12, 12, 12));

            imageLabel.setPreferredSize(new Dimension(120, 90));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            add(imageLabel, BorderLayout.WEST);

            JPanel centerPanel = new JPanel(new BorderLayout(6, 6));
            centerPanel.setOpaque(false);

            JPanel titleRow = new JPanel(new BorderLayout(8, 0));
            titleRow.setOpaque(false);

            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 19));
            favoriteIconLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
            favoriteIconLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            titleRow.add(titleLabel, BorderLayout.CENTER);
            titleRow.add(favoriteIconLabel, BorderLayout.EAST);

            JPanel detailsGrid = new JPanel(new GridLayout(3, 1, 4, 4));
            detailsGrid.setOpaque(false);

            cuisineLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            statsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
            starsLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            tagsLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));

            detailsGrid.add(cuisineLabel);
            detailsGrid.add(statsLabel);
            detailsGrid.add(starsLabel);

            centerPanel.add(titleRow, BorderLayout.NORTH);
            centerPanel.add(detailsGrid, BorderLayout.CENTER);
            centerPanel.add(tagsLabel, BorderLayout.SOUTH);

            add(centerPanel, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends Recipe> list, Recipe recipe, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            imageLabel.setIcon(createCardImageIcon(recipe.getImagePath()));
            titleLabel.setText(recipe.getName());
            favoriteIconLabel.setText(recipe.isFavorite() ? "★" : "☆");
            cuisineLabel.setText(recipe.getCuisine() + " • " + recipe.getCategory());
            statsLabel.setText(recipe.getPrepTime() + " mins  •  " + recipe.getCalories() + " kcal");
            starsLabel.setText(getStarRating(recipe.getRating()) + "  " + recipe.getRating() + "/5");
            tagsLabel.setText(recipe.getTags().isEmpty() ? "No tags" : "Tags: " + String.join(", ", recipe.getTags()));

            if (isSelected) {
                setBackground(list.getSelectionBackground());
                titleLabel.setForeground(list.getSelectionForeground());
                cuisineLabel.setForeground(list.getSelectionForeground());
                statsLabel.setForeground(list.getSelectionForeground());
                starsLabel.setForeground(list.getSelectionForeground());
                tagsLabel.setForeground(list.getSelectionForeground());
                favoriteIconLabel.setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                titleLabel.setForeground(list.getForeground());
                cuisineLabel.setForeground(new Color(80, 80, 80));
                statsLabel.setForeground(list.getForeground());
                starsLabel.setForeground(list.getForeground());
                tagsLabel.setForeground(Color.GRAY);
                favoriteIconLabel.setForeground(recipe.isFavorite() ? new Color(210, 150, 0) : Color.GRAY);
            }

            return this;
        }
    }

    private ImageIcon createCardImageIcon(String path) {
        if (path == null || path.isBlank()) {
            return createPlaceholderIcon(120, 90);
        }

        File file = new File(path);
        if (!file.exists()) {
            return createPlaceholderIcon(120, 90);
        }

        ImageIcon icon = new ImageIcon(path);
        Image image = getScaledImage(icon.getImage(), 120, 90);
        return new ImageIcon(image);
    }

    private String getStarRating(int rating) {
        StringBuilder stars = new StringBuilder();

        for (int i = 1; i <= 5; i++) {
            stars.append(i <= rating ? "★" : "☆");
        }

        return stars.toString();
    }
}



