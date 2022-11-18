BUILD_DIR := build

default: jlox

clean:
	@ rm -rf $(BUILD_DIR)

generate_ast:
	@ $(MAKE) -f util/java.make DIR=java PACKAGE=tool
	@ java -cp $(BUILD_DIR)/java com.craftinginterpreters.tool.GenerateAst \
		java/com/craftinginterpreters/lox

jlox: generate_ast
	@ $(MAKE) -f util/java.make DIR=java PACKAGE=lox

.PHONY: clean default jlox

