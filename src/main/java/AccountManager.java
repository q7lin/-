import java.sql.*;

public class AccountManager {

    // 开户功能
    public boolean createAccount(String accountType, String holderName, String idCard, double initialDeposit, String contactInfo) {
        String sql = "INSERT INTO Account (account_type, holder_name, id_card, contact_info, balance, open_date) VALUES (?, ?, ?, ?, ?, CURDATE())";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accountType);  // 账户类型
            stmt.setString(2, holderName);   // 姓名
            stmt.setString(3, idCard);       // 身份证号
            stmt.setString(4, contactInfo);  // 联系方式
            stmt.setDouble(5, initialDeposit);  // 初始存款
            stmt.executeUpdate();  // 执行插入

            System.out.println("账户创建成功！");
            return true;  // 成功
        } catch (SQLException e) {
            System.err.println("开户失败：" + e.getMessage());
            return false;  // 失败
        }
    }

    public boolean isIdCardExists(String idCard) {
        // 在数据库中查询 idCard 是否存在，返回 true 或 false
        // 这里是一个示例，您需要根据实际情况编写数据库查询逻辑
        String query = "SELECT COUNT(*) FROM account WHERE id_card = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, idCard);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 查询账户功能
    public String queryAccount(String idCard, BankSystemUI ui) {
        String sql = "SELECT * FROM Account WHERE id_card = ?";
        StringBuilder accountInfo = new StringBuilder();

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, idCard);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                accountInfo.append("账户ID: ").append(rs.getInt("account_id")).append("\n")
                        .append("账户类型: ").append(rs.getString("account_type")).append("\n")
                        .append("余额: ").append(rs.getDouble("balance")).append("\n")
                        .append("状态: ").append(rs.getString("status")).append("\n")
                        .append("开户日期: ").append(rs.getDate("open_date")).append("\n");
                ui.displayMessage(accountInfo.toString());
            } else {
                ui.displayMessage("未找到该账户！");
            }
        } catch (SQLException e) {
            System.err.println("查询失败：" + e.getMessage());
        }
        return sql;
    }

    // 存款功能
//    public void deposit(String idCard, double amount) {
//        String sql = "UPDATE Account SET balance = balance + ? WHERE id_card = ?";
//        try (Connection conn = DatabaseUtil.getConnection();
//             PreparedStatement stmt = conn.prepareStatement(sql)) {
//            stmt.setDouble(1, amount);
//            stmt.setString(2, idCard);
//            int rows = stmt.executeUpdate();
//            if (rows > 0) {
//                System.out.println("存款成功！");
//            } else {
//                System.out.println("未找到该账户！");
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }

    public boolean deposit(String idCard, double amount) {
        String checkSql = "SELECT status FROM Account WHERE id_card = ?";
        String updateSql = "UPDATE Account SET balance = balance + ? WHERE id_card = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            // 检查账户状态
            checkStmt.setString(1, idCard);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                if ("frozen".equals(status)) {
                    System.out.println("账户已冻结，无法存款！");
                    return false;
                }

                // 更新余额
                updateStmt.setDouble(1, amount);
                updateStmt.setString(2, idCard);
                int rows = updateStmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("存款成功！");
                    return true;
                } else {
                    System.out.println("未找到该账户！");
                    return false;
                }
            } else {
                System.out.println("未找到该账户！");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    // 取款功能
//    public void withdraw(String idCard, double amount) {
//        String checkSql = "SELECT balance FROM Account WHERE id_card = ?";
//        String updateSql = "UPDATE Account SET balance = balance - ? WHERE id_card = ?";
//        try (Connection conn = DatabaseUtil.getConnection();
//             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
//             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
//
//            // 检查账户余额
//            checkStmt.setString(1, idCard);
//            ResultSet rs = checkStmt.executeQuery();
//            if (rs.next()) {
//                double balance = rs.getDouble("balance");
//                if (balance >= amount) {
//                    // 更新余额
//                    updateStmt.setDouble(1, amount);
//                    updateStmt.setString(2, idCard);
//                    updateStmt.executeUpdate();
//                    System.out.println("取款成功！");
//                } else {
//                    System.out.println("余额不足，取款失败！");
//                }
//            } else {
//                System.out.println("未找到该账户！");
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
//    }

    public boolean withdraw(String idCard, double amount) {
        String checkSql = "SELECT status, balance, account_type FROM Account WHERE id_card = ?";
        String updateSql = "UPDATE Account SET balance = balance - ? WHERE id_card = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql);
             PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {

            // 检查账户状态和余额
            checkStmt.setString(1, idCard);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");
                double balance = rs.getDouble("balance");
                String accountType = rs.getString("account_type");

                // 检查账户是否被冻结
                if ("frozen".equals(status)) {
                    System.out.println("账户已冻结，无法取款！");
                    return false; // 冻结账户无法取款
                }

                // 如果是定期存款，允许取款，但没有利润
                if ("定期".equals(accountType)) {
                    System.out.println("取款成功！定期存款未到期，无法获得利润。");
                    // 即使没有利润，仍然允许取款
                }

                // 检查余额是否足够
                if (balance >= amount) {
                    // 执行取款操作
                    updateStmt.setDouble(1, amount);
                    updateStmt.setString(2, idCard);
                    updateStmt.executeUpdate();
                    System.out.println("取款成功！");
                    return true; // 取款成功
                } else {
                    System.out.println("余额不足，取款失败！");
                }
            } else {
                System.out.println("未找到该账户！");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // 如果没有满足条件，则返回失败
    }

    //利息计算
    public double calculateInterest(String idCard, int years) {
        // 假设当前年利率为 2.75%
        final double interestRate = 0.0275;

        String checkSql = "SELECT balance, account_type, status FROM Account WHERE id_card = ?";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(checkSql)) {

            stmt.setString(1, idCard);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String accountType = rs.getString("account_type");
                String status = rs.getString("status"); // 获取账户状态
                double balance = rs.getDouble("balance");

                // 检查账户是否被冻结
                if ("frozen".equals(status)) {
                    System.out.println("账户已被冻结，无法计算利息！");
                    return -1; // 账户冻结，无法计算利息
                }

                // 只对定期存款计算利息
                if ("定期".equals(accountType)) {
                    // 计算利息 = 本金 * 年利率 * 年数
                    double interest = balance * interestRate * years;
                    double totalAmount = balance + interest;
                    System.out.println("本息总额： " + totalAmount);
                    return totalAmount; // 返回计算后的本息
                } else {
                    System.out.println("该账户不是定期存款，无法计算利息！");
                    return -1; // 如果不是定期存款，返回 -1
                }
            } else {
                System.out.println("未找到该账户！");
                return -1;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }


    //转账功能
    public boolean transfer(String fromIdCard, String toIdCard, double amount) {
        String checkFromAccountSql = "SELECT status, account_type, balance FROM Account WHERE id_card = ?";
        String checkToAccountSql = "SELECT status, account_type FROM Account WHERE id_card = ?";
        String updateFromAccountSql = "UPDATE Account SET balance = balance - ? WHERE id_card = ?";
        String updateToAccountSql = "UPDATE Account SET balance = balance + ? WHERE id_card = ?";

        try (Connection conn = DatabaseUtil.getConnection()) {
            // 设置事务为手动提交，确保转账过程中不会出现问题
            conn.setAutoCommit(false);

            try (PreparedStatement checkFromStmt = conn.prepareStatement(checkFromAccountSql);
                 PreparedStatement checkToStmt = conn.prepareStatement(checkToAccountSql);
                 PreparedStatement updateFromStmt = conn.prepareStatement(updateFromAccountSql);
                 PreparedStatement updateToStmt = conn.prepareStatement(updateToAccountSql)) {

                // 检查转出账户
                checkFromStmt.setString(1, fromIdCard);
                ResultSet fromRs = checkFromStmt.executeQuery();
                if (!fromRs.next()) {
                    System.out.println("转出账户不存在！");
                    return false;
                }
                String fromStatus = fromRs.getString("status");
                String fromType = fromRs.getString("account_type");
                double fromBalance = fromRs.getDouble("balance");

                if ("frozen".equals(fromStatus)) {
                    System.out.println("转出账户已被冻结，无法转账！");
                    return false;
                }
                if (!"活期".equalsIgnoreCase(fromType)) {
                    System.out.println("转出账户不是活期账户，无法进行转账！");
                    return false;
                }
                if (fromBalance < amount) {
                    System.out.println("转出账户余额不足，无法进行转账！");
                    return false;
                }

                // 检查转入账户
                checkToStmt.setString(1, toIdCard);
                ResultSet toRs = checkToStmt.executeQuery();
                if (!toRs.next()) {
                    System.out.println("转入账户不存在！");
                    return false;
                }
                String toStatus = toRs.getString("status");
                String toType = toRs.getString("account_type");

                if ("frozen".equals(toStatus)) {
                    System.out.println("转入账户已被冻结，无法接收转账！");
                    return false;
                }
                if (!"活期".equalsIgnoreCase(toType)) {
                    System.out.println("fromType: " + fromType + ", toType: " + toType);
                    System.out.println("转入账户不是活期账户，无法接收转账！");
                    return false;
                }

                // 执行转账：扣减转出账户余额，增加转入账户余额
                updateFromStmt.setDouble(1, amount);
                updateFromStmt.setString(2, fromIdCard);
                int rowsFrom = updateFromStmt.executeUpdate();

                updateToStmt.setDouble(1, amount);
                updateToStmt.setString(2, toIdCard);
                int rowsTo = updateToStmt.executeUpdate();

                if (rowsFrom > 0 && rowsTo > 0) {
                    conn.commit(); // 提交事务
                    System.out.println("转账成功！从账户 " + fromIdCard + " 转账 " + amount + " 元到账户 " + toIdCard);
                    return true;
                } else {
                    conn.rollback(); // 回滚事务
                    System.out.println("转账失败！请稍后再试。");
                    return false;
                }
            } catch (Exception e) {
                conn.rollback(); // 出现异常时回滚事务
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }


    // 查询余额功能
    public void queryAccountBalance(String idCard) {
        String sql = "SELECT balance FROM Account WHERE id_card = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idCard);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                System.out.println("账户余额: " + balance);
            } else {
                System.out.println("未找到该账户！");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 账户删除功能
    public boolean deleteAccount(String idCard) {
        String checkStatusSql = "SELECT status, balance FROM Account WHERE id_card = ?";
        String deleteSql = "DELETE FROM Account WHERE id_card = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkStatusSql);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {

            // 检查账户状态和余额
            checkStmt.setString(1, idCard);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                double balance = rs.getDouble("balance");
                if ("frozen".equals(status)) {
                    System.out.println("账户已被冻结，无法销户！");
                    return false;
                }
                if (balance > 0) {
                    System.out.println("账户余额不为零，无法销户！");
                    return false;
                }
                // 删除账户
                deleteStmt.setString(1, idCard);
                int rows = deleteStmt.executeUpdate();
                if (rows > 0) {
                    System.out.println("账户销户成功！");
                    return true;
                } else {
                    System.out.println("未找到该账户！");
                    return false;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 账户冻结功能
    public boolean freezeAccount(String idCard) {
        String sql = "UPDATE Account SET status = 'frozen' WHERE id_card = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idCard);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("账户已冻结！");
                return true;
            } else {
                System.out.println("未找到该账户！");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // 账户解冻功能
    public boolean unfreezeAccount(String idCard) {
        String sql = "UPDATE Account SET status = 'active' WHERE id_card = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idCard);
            int rows = stmt.executeUpdate();
            if (rows > 0) {
                System.out.println("账户已解冻！");
                return true;
            } else {
                System.out.println("未找到该账户！");
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
