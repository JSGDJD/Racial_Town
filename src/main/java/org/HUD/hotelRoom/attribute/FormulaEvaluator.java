package org.HUD.hotelRoom.attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 公式计算引擎
 * 支持自定义属性的公式计算
 */
public class FormulaEvaluator {
    
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(max|min|sqrt|pow|abs|floor|ceil)\\(");
    
    /**
     * 计算公式
     * @param formula 公式字符串
     * @param variables 变量映射表
     * @return 计算结果
     */
    public static double evaluate(String formula, Map<String, Double> variables) {
        try {
            if (formula == null || formula.length() > 1000) {
                throw new IllegalArgumentException("公式无效或过长");
            }
            
            String processed = replaceVariables(formula, variables);
            processed = processFunctions(processed);
            return evaluateExpression(processed);
        } catch (Exception e) {
            org.HUD.hotelRoom.HotelRoom.get().getLogger().warning(
                "[公式计算] 计算失败: " + formula + " - " + e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * 替换公式中的变量
     */
    private static String replaceVariables(String formula, Map<String, Double> variables) {
        Matcher matcher = VARIABLE_PATTERN.matcher(formula);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String varName = matcher.group(1);
            Double value = variables.get(varName);
            
            if (value == null) {
                org.HUD.hotelRoom.HotelRoom.get().getLogger().warning(
                    "[公式计算] 未知变量: " + varName + " 在公式: " + formula);
                value = 0.0;
            }
            
            matcher.appendReplacement(result, String.valueOf(value));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * 处理函数调用
     */
    private static String processFunctions(String expression) {
        // 防止无限循环：添加迭代次数限制
        int maxIterations = 100;
        int iterations = 0;
        
        // 递归处理嵌套函数
        while (expression.contains("max(") || expression.contains("min(") || 
               expression.contains("sqrt(") || expression.contains("pow(") ||
               expression.contains("abs(") || expression.contains("floor(") || 
               expression.contains("ceil(")) {
            
            if (++iterations > maxIterations) {
                throw new IllegalStateException("函数嵌套层次过深或存在循环");
            }
            
            Matcher matcher = FUNCTION_PATTERN.matcher(expression);
            if (matcher.find()) {
                String funcName = matcher.group(1);
                int startPos = matcher.start();
                int endPos = findMatchingParen(expression, matcher.end() - 1);
                
                if (endPos == -1) {
                    throw new IllegalArgumentException("括号不匹配");
                }
                
                String argsStr = expression.substring(matcher.end(), endPos);
                String result = evaluateFunction(funcName, argsStr);
                
                expression = expression.substring(0, startPos) + result + 
                            expression.substring(endPos + 1);
            } else {
                break;
            }
        }
        
        return expression;
    }
    
    /**
     * 查找匹配的右括号
     */
    private static int findMatchingParen(String str, int startPos) {
        int count = 1;
        for (int i = startPos + 1; i < str.length(); i++) {
            if (str.charAt(i) == '(') count++;
            else if (str.charAt(i) == ')') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }
    
    /**
     * 计算函数
     */
    private static String evaluateFunction(String funcName, String argsStr) {
        String[] args = argsStr.split(",");
        
        switch (funcName) {
            case "max":
                if (args.length != 2) throw new IllegalArgumentException("max需要2个参数");
                double a = evaluateExpression(args[0].trim());
                double b = evaluateExpression(args[1].trim());
                return String.valueOf(Math.max(a, b));
                
            case "min":
                if (args.length != 2) throw new IllegalArgumentException("min需要2个参数");
                a = evaluateExpression(args[0].trim());
                b = evaluateExpression(args[1].trim());
                return String.valueOf(Math.min(a, b));
                
            case "sqrt":
                if (args.length != 1) throw new IllegalArgumentException("sqrt需要1个参数");
                a = evaluateExpression(args[0].trim());
                return String.valueOf(Math.sqrt(a));
                
            case "pow":
                if (args.length != 2) throw new IllegalArgumentException("pow需要2个参数");
                a = evaluateExpression(args[0].trim());
                b = evaluateExpression(args[1].trim());
                return String.valueOf(Math.pow(a, b));
                
            case "abs":
                if (args.length != 1) throw new IllegalArgumentException("abs需要1个参数");
                a = evaluateExpression(args[0].trim());
                return String.valueOf(Math.abs(a));
                
            case "floor":
                if (args.length != 1) throw new IllegalArgumentException("floor需要1个参数");
                a = evaluateExpression(args[0].trim());
                return String.valueOf(Math.floor(a));
                
            case "ceil":
                if (args.length != 1) throw new IllegalArgumentException("ceil需要1个参数");
                a = evaluateExpression(args[0].trim());
                return String.valueOf(Math.ceil(a));
                
            default:
                throw new IllegalArgumentException("未知函数: " + funcName);
        }
    }
    
    /**
     * 计算数学表达式（支持 +, -, *, /, %, 括号）
     */
    private static double evaluateExpression(String expression) {
        expression = expression.trim().replaceAll("\\s+", "");
        
        // 处理负号
        expression = expression.replaceAll("(?<=[(+\\-*/%^])-", "0-");
        if (expression.startsWith("-")) {
            expression = "0" + expression;
        }
        
        return evaluateAddSub(expression);
    }
    
    /**
     * 计算加减法
     */
    private static double evaluateAddSub(String expr) {
        int level = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') level++;
            else if (c == '(') level--;
            else if (level == 0 && (c == '+' || c == '-')) {
                if (i == 0) continue; // 负号
                double left = evaluateAddSub(expr.substring(0, i));
                double right = evaluateMulDiv(expr.substring(i + 1));
                return c == '+' ? left + right : left - right;
            }
        }
        return evaluateMulDiv(expr);
    }
    
    /**
     * 计算乘除法
     */
    private static double evaluateMulDiv(String expr) {
        int level = 0;
        for (int i = expr.length() - 1; i >= 0; i--) {
            char c = expr.charAt(i);
            if (c == ')') level++;
            else if (c == '(') level--;
            else if (level == 0 && (c == '*' || c == '/' || c == '%')) {
                double left = evaluateMulDiv(expr.substring(0, i));
                double right = evaluateParentheses(expr.substring(i + 1));
                if (c == '*') return left * right;
                else if (c == '/') {
                    // 防止除零
                    if (Math.abs(right) < 0.0001) {
                        throw new ArithmeticException("除数不能为零");
                    }
                    return left / right;
                }
                else {
                    // 防止除零
                    if (Math.abs(right) < 0.0001) {
                        throw new ArithmeticException("除数不能为零");
                    }
                    return left % right;
                }
            }
        }
        return evaluateParentheses(expr);
    }
    
    /**
     * 处理括号
     */
    private static double evaluateParentheses(String expr) {
        expr = expr.trim();
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return evaluateExpression(expr.substring(1, expr.length() - 1));
        }
        return Double.parseDouble(expr);
    }
    
    /**
     * 创建变量映射
     */
    public static Map<String, Double> createVariableMap(
            double damage, 
            double health, 
            double maxHealth,
            double targetHealth,
            double targetMaxHealth,
            double targetArmor,
            int level,
            double distance,
            Map<String, Double> attributes) {
        
        Map<String, Double> variables = new HashMap<>();
        
        // 防止除零错误：检查 maxHealth
        if (maxHealth <= 0) {
            maxHealth = 20.0;
        }
        if (targetMaxHealth <= 0) {
            targetMaxHealth = 20.0;
        }
        
        // 基础变量
        variables.put("damage", damage);
        variables.put("health", health);
        variables.put("max_health", maxHealth);
        variables.put("health_percent", (health / maxHealth) * 100.0);
        variables.put("target_health", targetHealth);
        variables.put("target_max_health", targetMaxHealth);
        variables.put("target_armor", targetArmor);
        variables.put("level", (double) level);
        variables.put("distance", distance);
        
        // 属性变量
        if (attributes != null) {
            variables.putAll(attributes);
        }
        
        return variables;
    }
}
