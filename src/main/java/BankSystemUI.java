import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BankSystemUI extends JFrame {

    private JTextArea outputArea;
    private JTextField accountTypeField, holderNameField, idCardField, contactInfoField, depositField, withdrawField, transferFromField, transferToField, transferAmountField;
    private JButton createAccountButton, queryAccountButton, depositButton, withdrawButton, freezeButton, unfreezeButton, deleteAccountButton, transferButton;
    private AccountManager accountManager;

    public BankSystemUI() {
        setTitle("银行账户管理系统");
        setSize(700, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        accountManager = new AccountManager(); // 创建 AccountManager 实例

        // 初始化界面组件
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(10, 2));

        inputPanel.add(new JLabel("账户类型:"));
        accountTypeField = new JTextField();
        inputPanel.add(accountTypeField);

        inputPanel.add(new JLabel("开户人姓名:"));
        holderNameField = new JTextField();
        inputPanel.add(holderNameField);

        inputPanel.add(new JLabel("身份证号:"));
        idCardField = new JTextField();
        inputPanel.add(idCardField);

        inputPanel.add(new JLabel("联系方式:"));
        contactInfoField = new JTextField();
        inputPanel.add(contactInfoField);

        inputPanel.add(new JLabel("存款金额:"));
        depositField = new JTextField();
        inputPanel.add(depositField);

        inputPanel.add(new JLabel("取款金额:"));
        withdrawField = new JTextField();
        inputPanel.add(withdrawField);

        inputPanel.add(new JLabel("转出账户身份证号:"));
        transferFromField = new JTextField();
        inputPanel.add(transferFromField);

        inputPanel.add(new JLabel("转入账户身份证号:"));
        transferToField = new JTextField();
        inputPanel.add(transferToField);

        inputPanel.add(new JLabel("转账金额:"));
        transferAmountField = new JTextField();
        inputPanel.add(transferAmountField);

        add(inputPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        createAccountButton = new JButton("开户");
        createAccountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String accountType = accountTypeField.getText();
                String holderName = holderNameField.getText();
                String idCard = idCardField.getText();
                String contactInfo = contactInfoField.getText();
                double initialDeposit = Double.parseDouble(depositField.getText());

                if (accountManager.isIdCardExists(idCard)) {
                    outputArea.setText("身份证号已存在，请更换！");
                } else {
                    boolean success = accountManager.createAccount(accountType, holderName, idCard, initialDeposit, contactInfo);
                    outputArea.setText(success ? "账户创建成功!" : "账户创建失败!");
                }
            }
        });
        buttonPanel.add(createAccountButton);

        queryAccountButton = new JButton("查询账户");
        queryAccountButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String idCard = idCardField.getText();
                accountManager.queryAccount(idCard, BankSystemUI.this);
            }
        });
        buttonPanel.add(queryAccountButton);

        depositButton = new JButton("存款");
        depositButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String idCard = idCardField.getText();
                double amount = Double.parseDouble(depositField.getText());
                boolean success = accountManager.deposit(idCard, amount);
                outputArea.setText(success ? "存款成功！" : "存款失败！");
            }
        });
        buttonPanel.add(depositButton);

        withdrawButton = new JButton("取款");
        withdrawButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String idCard = idCardField.getText();  // 获取用户输入的身份证号
                try {
                    // 获取用户输入的取款金额
                    double amount = Double.parseDouble(withdrawField.getText());

                    // 调用withdraw方法处理取款逻辑
                    boolean success = accountManager.withdraw(idCard, amount);

                    // 获取账户类型，以便判断是否为定期存款
                    String checkSql = "SELECT account_type, status FROM Account WHERE id_card = ?";
                    try (Connection conn = DatabaseUtil.getConnection();
                         PreparedStatement stmt = conn.prepareStatement(checkSql)) {

                        stmt.setString(1, idCard);
                        ResultSet rs = stmt.executeQuery();

                        if (rs.next()) {
                            String accountType = rs.getString("account_type");
                            String status = rs.getString("status");

                            // 判断取款成功后显示相应提示
                            if (success) {
                                if ("定期".equals(accountType)) {
                                    outputArea.setText("取款成功！定期存款未到期，无法获得利润。");
                                } else {
                                    outputArea.setText("取款成功！");
                                }
                            } else {
                                if ("frozen".equals(status)) {
                                    outputArea.setText("账户已冻结，无法取款！");
                                } else {
                                    outputArea.setText("取款失败！余额不足或账户类型不允许取款。");
                                }
                            }
                        } else {
                            outputArea.setText("未找到该账户！");
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        outputArea.setText("查询账户类型失败！");
                    }

                } catch (NumberFormatException ex) {
                    // 处理输入无效金额的异常
                    outputArea.setText("请输入有效的金额！");
                }
            }
        });
        buttonPanel.add(withdrawButton);

        JButton interestButton = new JButton("利息计算");
        interestButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // 弹出对话框获取年份
                String inputYear = JOptionPane.showInputDialog(null, "请输入存款年数：", "输入年份", JOptionPane.QUESTION_MESSAGE);

                if (inputYear != null && !inputYear.trim().isEmpty()) {
                    try {
                        // 获取用户输入的年份
                        int years = Integer.parseInt(inputYear);

                        // 获取账户号
                        String idCard = idCardField.getText();

                        // 调用利息计算方法
                        double totalAmount = accountManager.calculateInterest(idCard, years);

                        // 显示计算结果
                        if (totalAmount != -1) {
                            outputArea.setText("存款年份：" + years + "年\n本息总额：" + totalAmount);
                        } else {
                            outputArea.setText("无法计算利息！账户冻结或该账户不是定期存款。");
                        }
                    } catch (NumberFormatException ex) {
                        outputArea.setText("请输入有效的年份！");
                    }
                }
            }
        });
        buttonPanel.add(interestButton);




        freezeButton = new JButton("冻结账户");
        freezeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String idCard = idCardField.getText();
                boolean success = accountManager.freezeAccount(idCard);
                outputArea.setText(success ? "账户已冻结！" : "冻结失败！");
            }
        });
        buttonPanel.add(freezeButton);

        unfreezeButton = new JButton("解冻账户");
        unfreezeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String idCard = idCardField.getText();
                boolean success = accountManager.unfreezeAccount(idCard);
                outputArea.setText(success ? "账户已解冻！" : "解冻失败！");
            }
        });
        buttonPanel.add(unfreezeButton);

        deleteAccountButton = new JButton("销户");
        deleteAccountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String idCard = idCardField.getText();
                boolean success = accountManager.deleteAccount(idCard);
                outputArea.setText(success ? "账户销户成功!" : "账户销户失败!");
            }
        });
        buttonPanel.add(deleteAccountButton);

        transferButton = new JButton("转账");
        transferButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fromIdCard = transferFromField.getText();
                String toIdCard = transferToField.getText();
                double amount = Double.parseDouble(transferAmountField.getText());
                boolean success = accountManager.transfer(fromIdCard, toIdCard, amount);
                outputArea.setText(success ? "转账成功！" : "转账失败！");
            }
        });
        buttonPanel.add(transferButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void displayMessage(String message) {
        outputArea.setText(message);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new BankSystemUI().setVisible(true);
            }
        });
    }
}
