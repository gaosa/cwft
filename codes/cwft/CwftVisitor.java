import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

class Type {
    String basic;
    Type type1;
    Type type2;

    // 0 : basic
    // 1 : array
    // 2 : dict
    // 3 : unkown / error
    // 4 : enum
    int wrap = 3; 
    @Override
    public String toString() {
        switch (wrap) {
        case 0:
        case 4:
            return basic;
        case 1:
            return "vector<" + type1 + ">";
        case 2:
            return "map<" + type1 + "," + type2 + ">";
        default:
            return "unknown";
        }
    }
    public Type() {

    }
    public Type(String basic_) {
        basic = basic_;
        wrap = 0;
    }
    public Type(Type type1_) {
        type1 = type1_;
        wrap = 1;
    }
    public Type(Type type1_, Type type2_) {
        type1 = type1_;
        type2 = type2_;
        wrap = 2;
    }
}

class Record {
    List<String> code = new LinkedList<>();
    Type type = null;// = new Type();
    boolean isType = false;
    public Record(Type type_) {
        if (type_ == null) {
            System.out.println("Error: type is null!");
            System.out.println("Code: " + code.get(0));
            System.exit(0);
        }
        type = type_;
    }
    public Record() {
        type = new Type();
    }
}

public class CwftVisitor extends SwiftBaseVisitor<Record> {

    // swift type to c++ type
    // basic type, like String -> string
    // declaration of class, struct, like A -> A
    // or typedef
    Map<String, Type> symbol_table_1 = new HashMap<>();
    
    // identifier to c++ type
    // like a -> int
    Map<String, Type> symbol_table_2 = new HashMap<>();

    // renaming of functions
    Map<String, String> symbol_table_3 = new HashMap<>();

    public void init() {
        symbol_table_3.put("count", "size()");
        symbol_table_3.put("append", "push_back");
        Type int_type = new Type("int");
        Type double_type = new Type("double");
        Type bool_type = new Type("bool");
        Type string_type = new Type("string");
        Type char_type = new Type("char");
        symbol_table_1.put("Int", int_type);
        symbol_table_1.put("Double", double_type);
        symbol_table_1.put("String", string_type);
        symbol_table_1.put("Character", char_type);
        symbol_table_1.put("Bool", bool_type);
        symbol_table_2.put("count", int_type);
    }

    // for "for ... in ... style"
    boolean need_insert = false;
    String insert_id = "";
    String insert_type = "";
    // for enum add symbol_table
    Type global_enum_type;

    // for tabs
    String tab = "    ";
    
    List<String> declaration_code = new LinkedList<>();
    List<String> main_code = new LinkedList<>();

    public void checkValid(String id) {
        if (symbol_table_2.get(id) != null) {
            System.out.println("Error: " + id + " already defined!");
            System.exit(0);
        }
    }

    @Override
    public Record visitTop_level(SwiftParser.Top_levelContext ctx) {
        init();
        // statement* 
        // visit every statement
        for (SwiftParser.StatementContext cctx: ctx.statement()) {
            Record res = visit(cctx);
            if (cctx.declaration() != null) {
                declaration_code.addAll(res.code);
            } else {
                main_code.addAll(res.code);
            }
        }

        System.out.println("#include \"helper.cpp\"");
        for (String str: declaration_code) {
            System.out.println(str);
        }
        System.out.println("int main() {");
        for (String str: main_code) {
            System.out.println(tab + str);
        }
        System.out.println(tab + "return 0;");
        System.out.println("}");
        return new Record();
    }

    @Override
    public Record visitExpression(SwiftParser.ExpressionContext ctx) {
        // prefix_expression binary_expression*
        // for ..< and ...
        if (ctx.binary_expression() != null &&
            ctx.binary_expression().size() == 1) {
            Record rec1 = visit(ctx.prefix_expression());
            Record rec2 = visit(ctx.binary_expression(0));
            if (rec2.code.size() > 1) {
                Record ret = new Record(rec1.type);
                ret.isType = rec1.isType;
                String code = rec1.code.get(0);
                String code2 = rec2.code.get(0);
                ret.code.add(code + " " + code2);
                ret.code.add(rec2.code.get(1));
                ret.code.add(code);
                ret.code.add(rec2.code.get(2));
                return ret;
            }
        }

        Record rec1 = visit(ctx.prefix_expression());
        //Record ret = new Record(rec1.type);
        //ret.isType = rec1.isType;
        Type type = rec1.type;
        boolean isType = rec1.isType;
        String code = rec1.code.get(0);
        for (SwiftParser.Binary_expressionContext cctx: ctx.binary_expression()) {
            Record rec = visit(cctx);
            code += " " + rec.code.get(0);
            type = rec.type;
            isType = rec.isType;
        }  
        Record ret = new Record(type);
        ret.isType = isType;
        ret.code.add(code);
        return ret;
    }

    @Override
    public Record visitPrefix_expression(SwiftParser.Prefix_expressionContext ctx) {
        // prefix_operator? postfix_expression
        String code = "";
        if (ctx.prefix_operator() != null && !ctx.prefix_operator().getText().equals("&")) {
            code = ctx.prefix_operator().getText();
        }
        Record rec = visit(ctx.postfix_expression());
        Record ret = new Record(rec.type);
        ret.isType = rec.isType;
        code += rec.code.get(0);
        ret.code.add(code);
        return ret;
    }

    @Override
    public Record visitBinary_express(SwiftParser.Binary_expressContext ctx) {
        // binary_operator prefix_expression
        String code = ctx.binary_operator().getText();
        Record rec = visit(ctx.prefix_expression());
        Record ret  = new Record(rec.type);
        ret.isType = rec.isType;
        ret.code.add(code + " " + rec.code.get(0));
        if (code.equals("...") ||
            code.equals("..<")) {
            ret.code.add(code);
            ret.code.add(rec.code.get(0));
        }
        return ret;
    }

    @Override
    public Record visitAssignment_express(SwiftParser.Assignment_expressContext ctx) {
        // assignment_operator prefix_expression
        String code = "= ";
        Record rec = visit(ctx.prefix_expression());
        Record ret  = new Record(rec.type);
        ret.isType = rec.isType;
        ret.code.add(code + rec.code.get(0));
        return ret;       
    }

    @Override
    public Record visitConditional_express(SwiftParser.Conditional_expressContext ctx) {
        // conditional_operator prefix_expression
        Record rec1 = visit(ctx.conditional_operator());
        Record rec2 = visit(ctx.prefix_expression());
        Record ret  = new Record(rec2.type);
        ret.isType = rec2.isType;
        ret.code.add(rec1.code.get(0) + " " + rec2.code.get(0));
        return ret;       
    }

    @Override
    public Record visitConditional_operator(SwiftParser.Conditional_operatorContext ctx) {
        // Question_mark expression Colon
        Record rec = visit(ctx.expression());
        Record ret  = new Record(rec.type);
        ret.isType = rec.isType;
        ret.code.add("? " + rec.code.get(0) + " :");
        return ret;       
    }

    @Override
    public Record visitPrimary_expression(SwiftParser.Primary_expressionContext ctx) {
        Record ret = super.visitPrimary_expression(ctx);
        return ret;
    }

    @Override
    public Record visitLiteral_express(SwiftParser.Literal_expressContext ctx) {
        // literal
        Record rec = visit(ctx.literal());
        Record ret = new Record(rec.type);
        ret.code.add(rec.code.get(0));
        ret.isType = rec.isType;
        return ret;
    }

    @Override
    public Record visitArray_literal_express(SwiftParser.Array_literal_expressContext ctx) {
        // array_literal
        return super.visitArray_literal_express(ctx);
    }

    @Override
    public Record visitDict_literal_express(SwiftParser.Dict_literal_expressContext ctx) {
        // dictionary_literal
        return super.visitDict_literal_express(ctx);
    }

    // could be [Int]  (a type)
    // could be [a]    (an identifier) 
    @Override
    public Record visitArray_literal(SwiftParser.Array_literalContext ctx) {
        // Left_mid array_literal_items? Right_mid
        if (ctx.array_literal_items() != null) {
            Record rec = visit(ctx.array_literal_items());
            Record ret = new Record(rec.type);
            // decide whether is type or literal
            if (rec.isType) {
                // type
                ret.code.add(ret.type.toString());
                ret.isType = true;
            } else {
                ret.code.add("{" + rec.code.get(0) + "}");
            }
            return ret;
        } else {
            Record ret = new Record(new Type(new Type()));
            ret.code.add("{}");
            return ret;
        }
    }

    // if code length is zero, litral item is type ([Int])
    // else is literal
    @Override
    public Record visitArray_literal_items(SwiftParser.Array_literal_itemsContext ctx) {
        // array_literal_item (Comma array_literal_item)*
        
        // check the situation of [Int], or [[Int]]
        if (ctx.array_literal_item().size() == 1) {
            Record rec = visit(ctx.array_literal_item(0));
            if (rec.isType) {
                Record ret = new Record(new Type(rec.type));
                ret.isType = true;
                return ret;
            }
        }

        // normal situation: [a] or [4]
        String code = "";
        Type type = null;
        for (SwiftParser.Array_literal_itemContext cctx: ctx.array_literal_item()) {
            Record rec = visit(cctx);
            code += rec.code.get(0) + ", ";
            type = rec.type;
        }
        Record ret = new Record(new Type(type));
        ret.code.add(code.substring(0, code.length() - 2));
        return ret;
    }

    @Override
    public Record visitArray_literal_item(SwiftParser.Array_literal_itemContext ctx) {
        // expression
        return visit(ctx.expression());
    }

    @Override
    public Record visitNormal_dict(SwiftParser.Normal_dictContext ctx) {
        // Left_mid dictionary_literal_items Right_mid
        Record rec = visit(ctx.dictionary_literal_items());
        Record ret = new Record(rec.type);
        if (rec.isType) {
            ret.code.add(ret.type.toString());
            ret.isType = true;
        } else {
            ret.code.add("{" + rec.code.get(0) + "}");
        }
        return ret;
    }

    @Override
    public Record visitEmpty_dict(SwiftParser.Empty_dictContext ctx) {
        // Left_mid Colon Right_mid
        Record ret = new Record(new Type(new Type(), new Type()));
        ret.code.add("{}");
        return ret;
    }

    @Override
    public Record visitDictionary_literal_items(SwiftParser.Dictionary_literal_itemsContext ctx) {
        // dictionary_literal_item (Comma dictionary_literal_item)*
        
        if (ctx.dictionary_literal_item().size() == 1) {
            Record rec = visit(ctx.dictionary_literal_item(0));
            if (rec.isType) {
                Record ret = new Record(rec.type);
                ret.isType = true;
                return ret;
            }
        }

        String code = "";
        Type type = null;
        for (SwiftParser.Dictionary_literal_itemContext cctx: ctx.dictionary_literal_item()) {
            Record rec = visit(cctx);
            code += rec.code.get(0) + ", ";
            type = rec.type;
        }
        Record ret = new Record(type);
        ret.code.add(code.substring(0, code.length() - 2));
        return ret;        
    }

    @Override
    public Record visitDictionary_literal_item(SwiftParser.Dictionary_literal_itemContext ctx) {
        // expression Colon expression
        Record rec1 = visit(ctx.expression(0));
        Record rec2 = visit(ctx.expression(1));
        Record ret = new Record(new Type(rec1.type, rec2.type));
        ret.isType = rec1.isType;
        ret.code.add("{" + rec1.code.get(0) + ", " + rec2.code.get(0) + "}");
        return ret;
    }

    @Override
    public Record visitParenthesized_expression(SwiftParser.Parenthesized_expressionContext ctx) {
        // Left_small expression_element_list? Right_small
        Record ret = new Record();
        if (ctx.expression_element_list() != null) {
            ret.code.add("(" + visit(ctx.expression_element_list()).code.get(0) + ")");
        } else {
            ret.code.add("()");
        }
        return ret;
    }

    @Override
    public Record visitExpression_element_list(SwiftParser.Expression_element_listContext ctx) {
        // expression_element (Comma expression_element)*
        Record ret  = new Record();
        String code = "";
        for (SwiftParser.Expression_elementContext cctx: ctx.expression_element()) {
            code += visit(cctx).code.get(0) + ", ";
        }
        ret.code.add(code.substring(0, code.length() - 2));
        return ret;
    }

    @Override
    public Record visitExpression_element_no_id(SwiftParser.Expression_element_no_idContext ctx) {
        // expression
        return visit(ctx.expression());
    }

    @Override
    public Record visitExpression_element_id(SwiftParser.Expression_element_idContext ctx) {
        // identifier Colon expression
        return visit(ctx.expression());
    }

    @Override
    public Record visitPrimary(SwiftParser.PrimaryContext ctx) {
        // primary_expression
        return visit(ctx.primary_expression());
    }

    @Override
    public Record visitPostfix_operation(SwiftParser.Postfix_operationContext ctx) {
        // postfix_expression postfix_operator
        Record rec = visit(ctx.postfix_expression());
        Record ret = new Record(rec.type);
        ret.isType = rec.isType;
        ret.code.add(rec.code.get(0) + ctx.postfix_operator().getText());
        ret.type = rec.type;
        return ret;
    }

    @Override
    public Record visitFunction_call_expression(SwiftParser.Function_call_expressionContext ctx) {
        // postfix_expression parenthesized_expression
        Record rec1 = visit(ctx.postfix_expression());
        Record rec2 = visit(ctx.parenthesized_expression());
        Record ret = new Record(rec1.type);
        ret.code.add(rec1.code.get(0) + rec2.code.get(0));
        return ret;
    }

    @Override
    public Record visitExplicit_member_expression(SwiftParser.Explicit_member_expressionContext ctx) {
        // postfix_expression Dot identifier
        Record rec1 = visit(ctx.postfix_expression());
        Record rec2 = visit(ctx.identifier());
        Record ret = new Record(rec2.type);
        if (ret.type.wrap == 4) {
            // if is enum
            ret.code.add(rec2.code.get(0));
        } else {
            ret.code.add(rec1.code.get(0) + "." + rec2.code.get(0));
        }
        return ret;      
    }

    @Override
    public Record visitSubscript_expression(SwiftParser.Subscript_expressionContext ctx) {
        // postfix_expression Left_mid expression Right_mid
        Record rec1 = visit(ctx.postfix_expression());
        Type type = null;
        if (rec1.type.wrap == 1) {
            // array
            type = rec1.type.type1;
        } else if (rec1.type.wrap == 2) {
            // dictionary
            type = rec1.type.type2;
        } else if (rec1.type.wrap == 0 && rec1.type.basic.equals("string")) {
            // string
            type = new Type("char");
        } else {
            System.out.println("Error: No subscript function for " + ctx.postfix_expression().getText());
            System.exit(0);
        }
        Record ret = new Record(type);
        ret.code.add(rec1.code.get(0) + "[" + visit(ctx.expression()).code.get(0) + "]");
        return ret;
    }

    @Override
    public Record visitStatement(SwiftParser.StatementContext ctx) {
        Record rec = super.visitStatement(ctx);
        Record ret = new Record(rec.type);
        if (ctx.expression() != null || ctx.control_transfer_statement() != null) {
            ret.code.add(rec.code.get(0) + ";");
        } else {
            ret.code.addAll(rec.code);
        }
        return ret;
    }

    @Override
    public Record visitLoop_statement(SwiftParser.Loop_statementContext ctx) {
        return super.visitLoop_statement(ctx);
    }

    @Override
    public Record visitFor_in_statement(SwiftParser.For_in_statementContext ctx) {
        // For pattern In expression where_clause? code_block
        Record ret = new Record();
        Record rec1 = visit(ctx.expression());
        if (rec1.code.size() > 1) {
            String begin = rec1.code.get(2);
            String end = rec1.code.get(3);
            String op = rec1.code.get(1).equals("..<") ? "<" : "<=";
            String id = visit(ctx.pattern()).code.get(0);
            String head = "for (int " + id + " = " + begin + "; " + id + " " + op + " " + end + "; ++" + id + ")";
            ret.code.add(head);
            ret.code.addAll(visit(ctx.code_block()).code);
            return ret;
        }
        String it_type = rec1.type + "::iterator";
        String head = "for (" + it_type + " it = " + rec1.code.get(0) + ".begin(); it != " + rec1.code.get(0) + ".end(); ++it)";
        need_insert = true;
        insert_id = visit(ctx.pattern()).code.get(0);
        if (rec1.type.wrap == 1) {
            symbol_table_2.put(insert_id, rec1.type.type1);
            insert_type = rec1.type.type1.toString();
        } else if (rec1.type.wrap == 2) {
            insert_type = "pair<" + rec1.type.type1 + ", " + rec1.type.type2 + ">";
        } else {
            System.out.println("Error: for_in statement has wrong expression!");
            System.exit(0);
        }
        ret.code.add(head);
        ret.code.addAll(visit(ctx.code_block()).code);
        return ret;
    }

    @Override
    public Record visitWhile_statement(SwiftParser.While_statementContext ctx) {
        // While condition code_block
        Record ret = new Record();
        ret.code.add("while (" + visit(ctx.condition()).code.get(0) + ")");
        ret.code.addAll(visit(ctx.code_block()).code);
        return ret;
    }

    @Override
    public Record visitCondition(SwiftParser.ConditionContext ctx) {
        // expression
        return visit(ctx.expression());
    }

    @Override
    public Record visitRepeat_while_statement(SwiftParser.Repeat_while_statementContext ctx) {
        // Repeat code_block While condition
        Record ret = new Record();
        ret.code.add("do");
        ret.code.addAll(visit(ctx.code_block()).code);
        ret.code.add("while (" + visit(ctx.condition()).code.get(0) + ");");
        return ret;
    }

    @Override
    public Record visitBranch_statement(SwiftParser.Branch_statementContext ctx) {
        Record ret = super.visitBranch_statement(ctx);
        return ret;
    }

    @Override
    public Record visitIf_statement(SwiftParser.If_statementContext ctx) {
        // If condition code_block else_clause?
        Record ret = new Record();
        ret.code.add("if (" + visit(ctx.condition()).code.get(0) + ")");
        ret.code.addAll(visit(ctx.code_block()).code);
        if (ctx.else_clause() != null) {
            ret.code.addAll(visit(ctx.else_clause()).code);
        }
        return ret;
    }

    @Override
    public Record visitElse_clause(SwiftParser.Else_clauseContext ctx) {
        Record rec = super.visitElse_clause(ctx);
        Record ret = new Record();
        ret.code.add("else");
        ret.code.addAll(rec.code);
        return ret;
    }

    @Override
    public Record visitGuard_statement(SwiftParser.Guard_statementContext ctx) {
        // Guard condition Else code_block
        System.out.println("Guard not implemented!");
        System.exit(0);
        return null;
    }

    @Override
    public Record visitSwitch_statement(SwiftParser.Switch_statementContext ctx) {
        // Switch expression Left_big switch_case* Right_big
        Record ret = new Record();
        ret.code.add("switch (" + visit(ctx.expression()).code.get(0) + ")");
        ret.code.add("{");
        for (SwiftParser.Switch_caseContext cctx: ctx.switch_case()) {
            for (String code: visit(cctx).code) {
                ret.code.add(tab + code);
            }
            //ret.code.addAll(visit(cctx).code);
        }
        ret.code.add("}");
        return ret;
    }

    @Override
    public Record visitCase_switch_case(SwiftParser.Case_switch_caseContext ctx) {
        // case_label statement+
        Record ret = new Record();
        Record rec = visit(ctx.case_label());
        ret.code.add(rec.code.get(0));
        for (SwiftParser.StatementContext cctx: ctx.statement()) {
            ret.code.add(visit(cctx).code.get(0));
        }
        int size = ret.code.size();
        if (ret.code.get(size - 1).equals("fallthrough;")) {
            ret.code.remove(size - 1);
        } else {
            ret.code.add("break;");
        }
        return ret;
    }

    @Override
    public Record visitDefault_switch_case(SwiftParser.Default_switch_caseContext ctx) {
        // default_label statement+
        Record ret = new Record();
        ret.code.add(ctx.default_label().getText());
        for (SwiftParser.StatementContext cctx: ctx.statement()) {
            ret.code.add(visit(cctx).code.get(0));
        }
        return ret;
    }

    @Override
    public Record visitCase_label(SwiftParser.Case_labelContext ctx) {
        // Case case_item Colon
        Record ret = new Record();
        ret.code.add("case " + visit(ctx.case_item()).code.get(0) + ":");
        return ret;
    }

    @Override
    public Record visitCase_item_literal(SwiftParser.Case_item_literalContext ctx) {
        // literal where_clause?
        Record ret = new Record();
        ret.code.add(ctx.literal().getText());
        return ret;
    }

    @Override
    public Record visitCase_item_id(SwiftParser.Case_item_idContext ctx) {
        // Dot identifier where_clause?
        Record ret = new Record();
        ret.code.add(ctx.identifier().getText());
        return ret;
    }

    @Override
    public Record visitDefault_label(SwiftParser.Default_labelContext ctx) {
        // Default Colon
        return null;
    }

    @Override
    public Record visitWhere_clause(SwiftParser.Where_clauseContext ctx) {
        // Where expression
        return null;
    }

    @Override
    public Record visitBreak_trans(SwiftParser.Break_transContext ctx) {
        // Break
        Record ret = new Record();
        ret.code.add("break");
        return ret;
    }

    @Override
    public Record visitContinue_trans(SwiftParser.Continue_transContext ctx) {
        // Continue
        Record ret = new Record();
        ret.code.add("continue");
        return ret;
    }

    @Override
    public Record visitFallthrough_trans(SwiftParser.Fallthrough_transContext ctx) {
        // Fallthrough
        Record ret = new Record();
        ret.code.add("fallthrough");
        return ret;
    }

    @Override
    public Record visitReturn_trans(SwiftParser.Return_transContext ctx) {
        // Return expression
        Record ret = new Record();
        ret.code.add("return " + visit(ctx.expression()).code.get(0));
        return ret;        
    }

    // declaration part
    @Override
    public Record visitConstant_declaration(SwiftParser.Constant_declarationContext ctx) {
        // Let pattern_initializer_list
        Record rec = visit(ctx.pattern_initializer_list());
        Record ret = new Record();
        for (String code: rec.code) {
            ret.code.add("const " + code + ";");
        }
        return ret;
    }

    @Override
    public Record visitVariable_declaration(SwiftParser.Variable_declarationContext ctx) {
        // Var pattern_initializer_list
        Record rec = visit(ctx.pattern_initializer_list());
        Record ret = new Record();
        for (String code: rec.code) {
            ret.code.add(code + ";");
        }
        return ret;
    }

    @Override
    public Record visitTypealias_declaration(SwiftParser.Typealias_declarationContext ctx) {
        // Typealias identifier Assign type
        Record ret = new Record();
        Type type = visit(ctx.type()).type;
        String id = ctx.identifier().getText();
        symbol_table_1.put(id, type);
        ret.code.add("typedef " + type + " " + id + ";");
        return ret;
    }

    @Override
    public Record visitFunction_declaration(SwiftParser.Function_declarationContext ctx) {
        // function_head function_name function_signature function_body
        Record ret = new Record();
        Record rec_name = visit(ctx.function_name());
        Record rec_signature = visit(ctx.function_signature());
        Record rec_body = visit(ctx.function_body());
        Type func_type = rec_signature.type;                // return type
        String func_param = rec_signature.code.get(0);      // "(" + param + ")"
        String func_id = rec_name.code.get(0);              // func name
        if (func_type.wrap != 3) {
            symbol_table_2.put(func_id, func_type);
            ret.code.add(func_type + " " + func_id + func_param);
        } else {
            ret.code.add("void " + func_id + " " + func_param);
        }
        ret.code.addAll(rec_body.code);
        return ret;
    }

    @Override
    public Record visitEnum_declaration(SwiftParser.Enum_declarationContext ctx) {
        // Enum enum_name Left_big enum_member+ Right_big
        Record ret = new Record();

        Type type = new Type();
        String enum_id = ctx.enum_name().getText();
        type.wrap = 4;
        type.basic = enum_id;
        symbol_table_1.put(enum_id, type);

        ret.code.add("enum " + enum_id);
        ret.code.add("{");

        String code = "";
        global_enum_type = type;
        for (SwiftParser.Enum_memberContext cctx: ctx.enum_member()) {
            code += visit(cctx).code.get(0);
        }
        ret.code.add(tab + code);

        ret.code.add("};");
        return ret;
    }

    @Override
    public Record visitStruct_declaration(SwiftParser.Struct_declarationContext ctx) {
        // Struct struct_name struct_body
        Record ret = new Record();

        Record rec_name = visit(ctx.struct_name());
        Record rec_body = visit(ctx.struct_body());

        String struct_id = rec_name.code.get(0);
        ret.code.add("class " + struct_id);

        Type type = new Type();
        type.wrap = 0;
        type.basic = struct_id;
        symbol_table_1.put(struct_id, type);

        ret.code.addAll(rec_body.code);
        //ret.code.add(";");
        return ret;
    }

    @Override
    public Record visitClass_declaration(SwiftParser.Class_declarationContext ctx) {
        // Class class_name class_body
        Record ret = new Record();
        Record rec_name = visit(ctx.class_name());
        Record rec_body = visit(ctx.class_body());

        String class_id = rec_name.code.get(0);
        Type type = new Type();
        type.wrap = 0;
        type.basic = class_id;
        ret.code.add("class " + class_id);
        symbol_table_1.put(class_id, type);

        ret.code.addAll(rec_body.code);
        //ret.code.add(";");
        return ret;
    }

    @Override
    public Record visitCode_block(SwiftParser.Code_blockContext ctx) {
        // Left_big statement* Right_big
        Record ret = new Record();
        ret.code.add("{");
        if (need_insert) {
            need_insert = false;
            ret.code.add(tab + insert_type + " " + insert_id + " = *it;");
        }
        for (SwiftParser.StatementContext cctx: ctx.statement()) {
            Record rec = visit(cctx);
            for (String code: rec.code) {
                ret.code.add(tab + code);
            }
            //ret.code.addAll(rec.code);
        }
        ret.code.add("}");
        return ret;
    }

    @Override 
    public Record visitPattern_initializer_list(SwiftParser.Pattern_initializer_listContext ctx) {
        // pattern_initializer_list : pattern_initializer (Comma pattern_initializer)*;
        // visit every pattern_initializer and combine them into one list
        Record ret = new Record();
        for (SwiftParser.Pattern_initializerContext cctx: ctx.pattern_initializer()) {
            Record rec = visit(cctx);
            ret.code.add(rec.code.get(0));
            ret.type = rec.type;
        }
        return ret;
    }

    @Override
    public Record visitPattern_initializer(SwiftParser.Pattern_initializerContext ctx) {
        // pattern_initializer : pattern initializer?;
        // first visit pattern, get its type and identifier
        Record pattern_res = visit(ctx.pattern());
        String pattern_id = pattern_res.code.get(0);
        // make sure has not been defined
        if (symbol_table_1.get(pattern_id) != null) {
            System.out.println(pattern_id + " is a type!");
            System.exit(0);
        }
        checkValid(pattern_id);
        Type pattern_type = pattern_res.type;  // maybe null
        String code = "";
        Type type;
        if (ctx.initializer() != null) {
            // if have initializer
            Record init_res = visit(ctx.initializer());
            if (pattern_type.wrap != 3) {
                // if pattern has type, use it
                code = pattern_type + " " + pattern_id + " " + init_res.code.get(0);
                type = pattern_type;
            } else {
                // uses the initializer type
                code = init_res.type + " " + pattern_id + " " + init_res.code.get(0);
                type = init_res.type;
            }
        } else {
            // if no initializer
            // then pattern_type must not be null
            if (pattern_type.wrap == 3) {
                System.out.println("Declaration of " + pattern_id + " has neither type annotation nor initializer");
                System.exit(0);
            }
            code = pattern_type + " " + pattern_id;
            type = pattern_type;
        }
        // add to symbol table
        Record ret = new Record(type);
        symbol_table_2.put(pattern_id, type);
        ret.code.add(code);
        return ret;
    }

    @Override 
    public Record visitInitializer(SwiftParser.InitializerContext ctx) {
        // Assign expression
        Record rec = visit(ctx.expression());
        Record ret = new Record(rec.type);
        ret.code.add("= " + rec.code.get(0));
        return ret;
    }

    @Override
    public Record visitFunction_name(SwiftParser.Function_nameContext ctx) {
        // identifier
        Record ret = new Record();
        Record rec = visit(ctx.identifier());
        if (rec.type.wrap != 3) {
            System.out.println(rec.code.get(0) + " already defined!");
        }
        ret.code.add(rec.code.get(0));
        return ret;
    }

    @Override
    public Record visitFunction_signature(SwiftParser.Function_signatureContext ctx) {
        // parameter_clause function_result?
        Record ret = new Record();
        // decide type
        if (ctx.function_result() != null) {
            ret.type = visit(ctx.function_result()).type;
        }
        ret.code.add(visit(ctx.parameter_clause()).code.get(0));
        return ret;
    }

    @Override
    public Record visitFunction_result(SwiftParser.Function_resultContext ctx) {
        // Arrow type
        return new Record(visit(ctx.type()).type);
    }

    @Override
    public Record visitFunction_body(SwiftParser.Function_bodyContext ctx) {
        // code_block
        return visit(ctx.code_block());
    }

    @Override
    public Record visitParameter_clause(SwiftParser.Parameter_clauseContext ctx) {
        // Left_small parameter_list? Right_small
        Record ret = new Record();
        String params = "";
        if (ctx.parameter_list() != null) {
            params = "(" + visit(ctx.parameter_list()).code.get(0) + ")";
        } else {
            params = "()";
        }
        ret.code.add(params);
        return ret;
    }

    @Override
    public Record visitParameter_list(SwiftParser.Parameter_listContext ctx) {
        // parameter (Comma parameter)*
        Record ret = new Record();
        String str = "";
        for (SwiftParser.ParameterContext cctx: ctx.parameter()) {
            str += visit(cctx).code.get(0) + ", ";
        }
        ret.code.add(str.substring(0, str.length() - 2));
        return ret;
    }

    @Override
    public Record visitParameter(SwiftParser.ParameterContext ctx) {
        // external_parameter_name? local_parameter_name type_annotation
        Record ret = new Record();
        //System.out.println(ctx.getText());
        Record rec = visit(ctx.type_annotation());
        Type type = rec.type;
        String code = ctx.local_parameter_name().getText();
        if (rec.code.size() == 0) {
            ret.code.add(type + " " + code);
        } else {
            ret.code.add(type + " & " + code);
        }
        checkValid(code);
        symbol_table_2.put(code, type);
        return ret;
    }

    @Override
    public Record visitEnum_member(SwiftParser.Enum_memberContext ctx) {
        // Case enum_case_list
        return visit(ctx.enum_case_list());
    }

    @Override
    public Record visitEnum_case_list(SwiftParser.Enum_case_listContext ctx) {
        // enum_case (Comma enum_case)*
        String code = "";
        for (SwiftParser.Enum_caseContext cctx: ctx.enum_case()) {
            code += cctx.getText() + ",";
            checkValid(cctx.getText());
            symbol_table_2.put(cctx.getText(), global_enum_type);
        }
        Record ret = new Record();
        ret.code.add(code);
        return ret;
    }

    @Override
    public Record visitStruct_name(SwiftParser.Struct_nameContext ctx) {
        // identifier
        Record ret = new Record();
        ret.code.add(ctx.identifier().getText());
        return ret;
    }

    @Override
    public Record visitStruct_body(SwiftParser.Struct_bodyContext ctx) {
        // Left_big declaration* Right_big
        Record ret = new Record();
        ret.code.add("{");
        ret.code.add("public:");
        for (SwiftParser.DeclarationContext cctx: ctx.declaration()) {
            for (String code: visit(cctx).code) {
                ret.code.add(tab + code);
            }
            //ret.code.addAll(visit(cctx).code);
        }
        ret.code.add("};");
        return ret;
    }

    @Override
    public Record visitClass_name(SwiftParser.Class_nameContext ctx) {
        // identifier
        Record ret = new Record();
        ret.code.add(ctx.identifier().getText());
        return ret;
    }

    @Override
    public Record visitClass_body(SwiftParser.Class_bodyContext ctx) {
        // Left_big declaration* Right_big
        Record ret = new Record();
        ret.code.add("{");
        ret.code.add("public:");
        for (SwiftParser.DeclarationContext cctx: ctx.declaration()) {
            for (String code: visit(cctx).code) {
                ret.code.add(tab + code);
            }
            //ret.code.addAll(visit(cctx).code);
        }
        ret.code.add("};");
        return ret;
    }

    @Override
    public Record visitPattern(SwiftParser.PatternContext ctx) {
        // pattern : identifier type_annotation?
        Record ret = new Record();
        ret.code.add(visit(ctx.identifier()).code.get(0));
        if (ctx.type_annotation() != null) {
            ret.type = visit(ctx.type_annotation()).type;
        }
        return ret;
    }

    @Override
    public Record visitType_annotation(SwiftParser.Type_annotationContext ctx) {
        // type_annotation : Colon Inout? type;
        Record ret = visit(ctx.type());
        //System.out.println(ctx.getText());
        if (ctx.Inout() != null) {
            ret.code.add("&");
        }
        return ret;
    }

    @Override
    public Record visitArray_type(SwiftParser.Array_typeContext ctx) {
        // array_type : Left_mid type Right_mid
        return new Record(new Type(visit(ctx.type()).type));
    }

    @Override
    public Record visitDictionary_type(SwiftParser.Dictionary_typeContext ctx) {
        // dictionary_type : Left_mid type Colon type Right_mid
        return new Record(new Type(visit(ctx.type(0)).type, visit(ctx.type(1)).type));
    }

    @Override
    public Record visitBasic_type(SwiftParser.Basic_typeContext ctx) {
        return super.visitBasic_type(ctx);
    }

    @Override
    public Record visitType_identifier(SwiftParser.Type_identifierContext ctx) {
        // type_identifier : identifier
        Record rec = visit(ctx.identifier());
        return new Record(rec.type);
    }

    @Override
    public Record visitIdentifier(SwiftParser.IdentifierContext ctx) {
        // identifier : Identifier
        String id = ctx.Identifier().getText();
        Type type = symbol_table_1.get(id);
        Record ret;
        if (type != null) {
            // it is a type
            ret = new Record(type);
            ret.isType = true;
            ret.code.add(type.toString());
            return ret;
        }
        type = symbol_table_2.get(id);
        if (type != null) {
            // it is a variable
            ret = new Record(type);
            String rename = symbol_table_3.get(id);
            if (rename != null) {
                ret.code.add(rename);
            } else {
                ret.code.add(id);
            }
            return ret;
        }
        ret = new Record();
        String rename = symbol_table_3.get(id);
        if (rename != null) {
            ret.code.add(rename);
        } else {
            ret.code.add(id);
        }
        return ret;
    }

    @Override
    public Record visitNum_literal(SwiftParser.Num_literalContext ctx) {
        Record ret = new Record();
        ret.code.add(ctx.Numeric_literal().getText());
        ret.type.basic = "int";
        ret.type.wrap = 0;
        return ret;
    }

    @Override
    public Record visitStr_literal(SwiftParser.Str_literalContext ctx) {
        Record ret = new Record();
        ret.code.add(ctx.String_literal().getText());
        ret.type.basic = "string";
        ret.type.wrap = 0;
        return ret;
    }

    @Override
    public Record visitBool_literal(SwiftParser.Bool_literalContext ctx) {
        Record ret = new Record();
        ret.code.add(ctx.Boolean_literal().getText());
        ret.type.basic = "bool";
        ret.type.wrap = 0;
        return ret;
    }

}
