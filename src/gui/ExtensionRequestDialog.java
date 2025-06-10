package gui;

import models.ExtensionRequest;
import models.User;
import services.ExtensionRequestService;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExtensionRequestDialog extends JDialog {
    private User currentUser;
    private ExtensionRequestService extensionRequestService = new ExtensionRequestService();

    private DefaultListModel<ExtensionRequest> requestsListModel;
    private JList<ExtensionRequest> requestsList;
    private JButton closeButton;
    private JButton refreshButton;

    public ExtensionRequestDialog(Frame parent, User user) {
        super(parent, "Historia próśb o przedłużenie", true);
        this.currentUser = user;
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadRequests();
    }

    private void initializeComponents() {
        setSize(700, 500);
        setLocationRelativeTo(getParent());
        setResizable(true);

        requestsListModel = new DefaultListModel<>();
        requestsList = new JList<>(requestsListModel);
        requestsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestsList.setCellRenderer(new ExtensionRequestCellRenderer());

        closeButton = new JButton("Zamknij");
        refreshButton = new JButton("Odśwież");
    }

    private void setupLayout() {
        setLayout(new BorderLayout());

        // Panel górny z informacjami
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Historia próśb o przedłużenie wypożyczeń dla: " + currentUser.getUsername()));
        add(topPanel, BorderLayout.NORTH);

        // Lista próśb
        add(new JScrollPane(requestsList), BorderLayout.CENTER);

        // Panel z przyciskami
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(refreshButton);
        buttonPanel.add(closeButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void setupEventHandlers() {
        closeButton.addActionListener(e -> dispose());
        refreshButton.addActionListener(e -> loadRequests());

        // Obsługa podwójnego kliknięcia
        requestsList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    showRequestDetails();
                }
            }
        });
    }

    private void loadRequests() {
        requestsListModel.clear();
        List<ExtensionRequest> requests = extensionRequestService.getUserExtensionRequests(currentUser.getId());

        for (ExtensionRequest request : requests) {
            requestsListModel.addElement(request);
        }

        if (requests.isEmpty()) {
            // Dodaj informacyjny element
            requestsListModel.addElement(createInfoRequest("Brak próśb o przedłużenie"));
        }
    }

    private ExtensionRequest createInfoRequest(String message) {
        ExtensionRequest infoRequest = new ExtensionRequest();
        infoRequest.setBookTitle(message);
        infoRequest.setBookAuthor("");
        infoRequest.setStatus("INFO");
        return infoRequest;
    }

    private void showRequestDetails() {
        ExtensionRequest selectedRequest = requestsList.getSelectedValue();
        if (selectedRequest != null && !"INFO".equals(selectedRequest.getStatus())) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

            String details = String.format(
                    "Książka: %s\nAutor: %s\nLiczba dni: %d\nData prośby: %s\nStatus: %s",
                    selectedRequest.getBookTitle(),
                    selectedRequest.getBookAuthor(),
                    selectedRequest.getRequestedDays(),
                    selectedRequest.getRequestDate().format(formatter),
                    getStatusText(selectedRequest.getStatus())
            );

            if (selectedRequest.getAdminDecisionDate() != null) {
                details += "\nData decyzji: " + selectedRequest.getAdminDecisionDate().format(formatter);
            }

            if (selectedRequest.getAdminComment() != null && !selectedRequest.getAdminComment().trim().isEmpty()) {
                details += "\nKomentarz administratora: " + selectedRequest.getAdminComment();
            }

            String title = "Szczegóły prośby o przedłużenie";
            int messageType = JOptionPane.INFORMATION_MESSAGE;

            if ("REJECTED".equals(selectedRequest.getStatus())) {
                messageType = JOptionPane.WARNING_MESSAGE;
                title = "Prośba odrzucona";
            } else if ("APPROVED".equals(selectedRequest.getStatus())) {
                messageType = JOptionPane.INFORMATION_MESSAGE;
                title = "Prośba zatwierdzona";
            }

            JOptionPane.showMessageDialog(this, details, title, messageType);
        }
    }

    private String getStatusText(String status) {
        return switch (status) {
            case "PENDING" -> "Oczekuje na decyzję";
            case "APPROVED" -> "Zatwierdzona";
            case "REJECTED" -> "Odrzucona";
            default -> status;
        };
    }

    // Renderer dla kolorowania różnych statusów próśb
    private static class ExtensionRequestCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof ExtensionRequest request) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

                if ("INFO".equals(request.getStatus())) {
                    setText(request.getBookTitle());
                    if (!isSelected) {
                        setBackground(Color.LIGHT_GRAY);
                        setForeground(Color.DARK_GRAY);
                    }
                    return this;
                }

                String displayText = String.format("%s - %s (%d dni) - %s - %s",
                        request.getBookTitle(),
                        request.getBookAuthor(),
                        request.getRequestedDays(),
                        getStatusText(request.getStatus()),
                        request.getRequestDate().format(formatter)
                );

                if (!isSelected) {
                    switch (request.getStatus()) {
                        case "PENDING":
                            setBackground(new Color(255, 255, 200)); // Jasny żółty
                            setForeground(Color.BLACK);
                            break;
                        case "APPROVED":
                            setBackground(new Color(200, 255, 200)); // Jasny zielony
                            setForeground(Color.BLACK);
                            break;
                        case "REJECTED":
                            setBackground(new Color(255, 200, 200)); // Jasny czerwony
                            setForeground(Color.BLACK);
                            break;
                        default:
                            setBackground(Color.WHITE);
                            setForeground(Color.BLACK);
                    }
                }

                setText(displayText);
            }

            return this;
        }

        private String getStatusText(String status) {
            return switch (status) {
                case "PENDING" -> "Oczekuje";
                case "APPROVED" -> "Zatwierdzona";
                case "REJECTED" -> "Odrzucona";
                default -> status;
            };
        }
    }
}
