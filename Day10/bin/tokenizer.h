#pragma once

#include <string>
#include <vector>
#include <regex>

struct Token
{
	int type;
	std::string val;
};

struct TokenDef
{
	std::regex regex;
	int type;
	int precedence;
};

struct TokenMatch
{
	int type;
	std::string val;
	int start, end;
	int precedence;
};

namespace tokenizer
{
	extern std::vector<TokenDef> definitions;

	void tokenize(const std::string& src, std::vector<Token>& tokens);

	void findMatches(const std::string& input, const TokenDef& def, std::vector<TokenMatch>& matches);

	void addDefinition(int type, std::string regex, int precedence);

	void removeDefinitions();
}