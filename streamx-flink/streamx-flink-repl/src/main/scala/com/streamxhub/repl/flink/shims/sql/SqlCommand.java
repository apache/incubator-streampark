package com.streamxhub.repl.flink.shims.sql;


import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Supported SQL commands.
 *
 * @author benjobs
 */
public enum SqlCommand {
    QUIT("(QUIT|EXIT)", getNoOperands()),
    CLEAR("CLEAR", getNoOperands()),
    HELP("HELP", getNoOperands()),
    SHOW_CATALOGS("SHOW\\s+CATALOGS", getNoOperands()),
    SHOW_DATABASES("SHOW\\s+DATABASES", getNoOperands()),
    SHOW_TABLES("SHOW\\s+TABLES", getNoOperands()),
    SHOW_FUNCTIONS("SHOW\\s+FUNCTIONS", getNoOperands()),
    SHOW_MODULES("SHOW\\s+MODULES", getNoOperands()),
    USE_CATALOG("USE\\s+CATALOG\\s+(.*)", getSingleOperand()),
    USE("USE\\s+(?!CATALOG)(.*)", getSingleOperand()),
    CREATE_CATALOG(null, getSingleOperand()),
    DROP_CATALOG(null, getSingleOperand()),
    DESC("DESC\\s+(.*)", getSingleOperand()),
    DESCRIBE("DESCRIBE\\s+(.*)", getSingleOperand()),
    EXPLAIN("EXPLAIN\\s+(.*)", getSingleOperand()),
    CREATE_DATABASE("(CREATE\\s+DATABASE\\s+.*)", getSingleOperand()),
    DROP_DATABASE("(DROP\\s+DATABASE\\s+.*)", getSingleOperand()),
    ALTER_DATABASE("(ALTER\\s+DATABASE\\s+.*)", getSingleOperand()),
    CREATE_TABLE("(CREATE\\s+TABLE\\s+.*)", getSingleOperand()),
    DROP_TABLE("(DROP\\s+TABLE\\s+.*)", getSingleOperand()),
    ALTER_TABLE("(ALTER\\s+TABLE\\s+.*)", getSingleOperand()),
    DROP_VIEW("DROP\\s+VIEW\\s+(.*)", getSingleOperand()),
    RESET("RESET", getNoOperands()),
    SOURCE("SOURCE\\s+(.*)", getSingleOperand()),
    CREATE_FUNCTION(null, getSingleOperand()),
    DROP_FUNCTION(null, getSingleOperand()),
    ALTER_FUNCTION(null, getSingleOperand()),
    SELECT("(SELECT.*)", getSingleOperand()),
    INSERT_INTO("(INSERT\\s+INTO.*)", getSingleOperand()),
    INSERT_OVERWRITE("(INSERT\\s+OVERWRITE.*)", getSingleOperand()),
    CREATE_VIEW("CREATE\\s+VIEW\\s+(\\S+)\\s+AS\\s+(.*)",
            (operands) -> {
                if (operands.length < 2) {
                    return Optional.empty();
                }
                return Optional.of(new String[]{operands[0], operands[1]});
            }),
    SET(
            "SET(\\s+(\\S+)\\s*=(.*))?", // whitespace is only ignored on the left side of '='
            (operands) -> {
                if (operands.length < 3) {
                    return Optional.empty();
                } else if (operands[0] == null) {
                    return Optional.of(new String[0]);
                }
                return Optional.of(new String[]{operands[1], operands[2]});
            });

    public final Pattern pattern;
    public final Function<String[], Optional<String[]>> operandConverter;

    SqlCommand(String matchingRegex, Function<String[], Optional<String[]>> operandConverter) {
        if (matchingRegex == null) {
            this.pattern = null;
        } else {
            int DEFAULT_PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;
            this.pattern = Pattern.compile(matchingRegex, DEFAULT_PATTERN_FLAGS);
        }
        this.operandConverter = operandConverter;
    }


    private static Function<String[], Optional<String[]>> getNoOperands() {
        return (x) -> Optional.of(new String[0]);
    }

    private static Function<String[], Optional<String[]>> getSingleOperand() {
        return (operands) -> Optional.of(new String[]{operands[0]});
    }

    @Override
    public String toString() {
        return super.toString().replace('_', ' ');
    }

    public boolean hasOperands() {
        return operandConverter != getNoOperands();
    }
}
