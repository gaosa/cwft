grammar Swift;
top_level : statement*;
// expression
expression : prefix_expression binary_expression*;
prefix_expression : prefix_operator? postfix_expression;
binary_expression : binary_operator prefix_expression       # binary_express
                  | assignment_operator prefix_expression   # assignment_express
                  | conditional_operator prefix_expression  # conditional_express
                  ;
assignment_operator : Assign;
conditional_operator : Question_mark expression Colon;

primary_expression : identifier                       
                   | literal_expression         
                   | parenthesized_expression   
                   ;
literal_expression : literal              # literal_express              
                   | array_literal        # array_literal_express  
                   | dictionary_literal   # dict_literal_express    
                   ;
array_literal : Left_mid array_literal_items? Right_mid;
array_literal_items : array_literal_item (Comma array_literal_item)*;
array_literal_item : expression;
dictionary_literal : Left_mid dictionary_literal_items Right_mid  # normal_dict
                   | Left_mid Colon Right_mid                     # empty_dict
                   ;
dictionary_literal_items : dictionary_literal_item (Comma dictionary_literal_item)* Comma?;
dictionary_literal_item : expression Colon expression;

parenthesized_expression : Left_small expression_element_list? Right_small;
expression_element_list : expression_element (Comma expression_element)*;
expression_element : expression                   # expression_element_no_id
                   | identifier Colon expression  # expression_element_id
                   ;

postfix_expression : primary_expression                               # primary 
                   | postfix_expression postfix_operator              # postfix_operation
                   | postfix_expression parenthesized_expression      # function_call_expression
                   | postfix_expression Dot identifier                # explicit_member_expression
                   | postfix_expression Left_mid expression Right_mid # subscript_expression
                   ;
// statement
statement : expression Semicolon?
          | declaration Semicolon?
          | loop_statement Semicolon?
          | branch_statement Semicolon?
          | control_transfer_statement Semicolon?
          ;
loop_statement : for_in_statement
               | while_statement
               | repeat_while_statement
               ;
for_in_statement : For pattern In expression where_clause? code_block;
while_statement : While condition code_block;
condition : expression;
repeat_while_statement : Repeat code_block While condition;
branch_statement : if_statement
                 | guard_statement
                 | switch_statement
                 ;
if_statement : If condition code_block else_clause?;
else_clause : Else code_block
            | Else if_statement
            ;
guard_statement : Guard condition Else code_block;
switch_statement : Switch expression Left_big switch_case* Right_big;
switch_case : case_label statement+     # case_switch_case
            | default_label statement+  # default_switch_case
            ;
case_label : Case case_item Colon;
case_item : literal where_clause?         # case_item_literal
          | Dot identifier where_clause?  # case_item_id
          ;
default_label : Default Colon;
where_clause : Where expression;
control_transfer_statement : Break              # break_trans
                           | Continue           # continue_trans
                           | Fallthrough        # fallthrough_trans
                           | Return expression  # return_trans
                           ;
// declarations
declaration : Let pattern_initializer_list                                    # constant_declaration
            | Var pattern_initializer_list                                    # variable_declaration
            | Typealias identifier Assign type                                # typealias_declaration
            | function_head function_name function_signature function_body    # function_declaration
            | Enum enum_name Left_big enum_member+ Right_big                  # enum_declaration
            | Struct struct_name struct_body                                  # struct_declaration
            | Class class_name class_body                                     # class_declaration
            ;
code_block : Left_big statement* Right_big;
pattern_initializer_list : pattern_initializer (Comma pattern_initializer)*;
pattern_initializer : pattern initializer?;
initializer : Assign expression;
function_head : Func;
function_name : identifier;
function_signature : parameter_clause function_result?;
function_result : Arrow type;
function_body : code_block;
parameter_clause : Left_small parameter_list? Right_small;
parameter_list : parameter (Comma parameter)*;
parameter : external_parameter_name? local_parameter_name type_annotation;
external_parameter_name : identifier;
local_parameter_name : identifier;
enum_name : identifier;
enum_member : Case enum_case_list;
enum_case_list : enum_case (Comma enum_case)*;
enum_case : identifier;
struct_name : identifier;
struct_body : Left_big declaration* Right_big;
class_name : identifier;
class_body : Left_big declaration* Right_big;
// pattern
pattern : identifier type_annotation?;
type_annotation : Colon Inout? type;
// types
type : Left_mid type Right_mid              # array_type 
     | Left_mid type Colon type Right_mid   # dictionary_type
     | type_identifier                      # basic_type
     ;
//array_type : Left_mid type Right_mid;
//dictionary_type : Left_mid type Colon type Right_mid;
type_identifier : identifier;
// operator
binary_operator : Plus | Minus | Mul | Div | Rem | Eq | Neq | Gt | Lt | Ge | Le | And | Or | Shift_right | Shift_left | PlusEq | MinusEq | Half_open_range | Closed_range;
prefix_operator : Not
                | Double_plus
                | Double_minus
                | Plus
                | Minus
                | Reverse
                | Single_and
                ;
postfix_operator : Double_plus
                 | Double_minus 
                 ;
// identifier
identifier : Identifier;
// literal
literal : Numeric_literal   # num_literal
        | String_literal    # str_literal
        | Boolean_literal   # bool_literal
        ;
// lexical
Comment : '//' ~[\r\n]* [\r\n] -> skip;
// key words
Inout : 'inout';
Class : 'class';
Enum : 'enum';
Func : 'func';
Let : 'let';
Static : 'static';
Struct : 'struct';
Var : 'var';
Break : 'break';
Case : 'case';
Continue : 'continue';
Default : 'default';
Do : 'do';
Else : 'else';
Fallthrough : 'fallthrough'; 
For : 'for';
Guard : 'guard';
If : 'if';
In : 'in';
Return : 'return';
Repeat : 'repeat';
Switch : 'switch';
Where : 'where';
While : 'while';
Optional : 'optional';
Typealias : 'typealias';
// operator
//Operator : Arrow | Assign | Plus | Minus | Mul | Div | Rem | Eq | Neq | Gt | Lt | Ge | Le | Not | And | Or | Double_plus | Double_minus | Reverse | Shift_right | Shift_left;
Arrow : '->';
Assign : '=';
Plus : '+';
Minus : '-';
Mul : '*';
Div : '/';
Rem : '%';
Eq : '==';
Neq : '!=';
Gt : '>';
Lt : '<';
Ge : '>=';
Le : '<=';
Not : '!';
And : '&&';
Or : '||';
Reverse : '~';
Double_plus : '++';
PlusEq : '+=';
MinusEq : '-=';
Double_minus : '--';
Shift_right : '>>';
Shift_left : '<<';
Single_and : '&';
Half_open_range : '..<';
Closed_range : '...';
// comma, dot, brackets
Comma : ',';
Dot : '.';
Colon : ':';
Left_mid : '[';
Right_mid : ']';
Left_small : '(';
Right_small : ')';
Left_big : '{';
Right_big : '}';
Semicolon : ';';
Question_mark : '?';
// built-in types
//Int : 'Int';
//Double : 'Double';
//String : 'String';
//Bool : 'Bool';
// literal
Numeric_literal : '-'? Integer_literal | '-'? Floating_point_literal;
Boolean_literal : 'false' | 'true';
Integer_literal : Decimal_literal;
fragment Decimal_literal : [0-9]+;
Floating_point_literal : Decimal_literal Fraction? Exponent?;
fragment Fraction : '.' Decimal_literal;
fragment Exponent : [eE] [+-]? Decimal_literal;
String_literal : '"' String_Character* '"';
fragment String_Character : [a-zA-Z_ ] | [0-9] | [!@#$%&*()] | '^' | '-' | [+=~`{}|] | '[' | ']' | [:;,.<>?/];
// identifier
Identifier : Identifier_head Identifier_character*;
fragment Identifier_head : [a-zA-Z_];
fragment Identifier_character : Identifier_head | [0-9];
WS : [ \r\n\t] -> skip; 