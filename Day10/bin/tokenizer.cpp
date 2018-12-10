#include "tokenizer.h"

#include <regex>
#include <map>
#include <algorithm>

namespace tokenizer
{
	std::vector<TokenDef> definitions;

	void tokenize(const std::string& src, std::vector<Token>& tokens)
	{
		std::vector<TokenMatch> matches;
		for (int i = 0; i < definitions.size(); i++)
		{
			findMatches(src, definitions[i], matches);
		}

		std::map<int, std::vector<TokenMatch>> groupedMatches;
		for (const TokenMatch& m : matches)
		{
			groupedMatches[m.start].push_back(m);
		}

		const TokenMatch* lastMatch = nullptr;
		for (std::pair<int, std::vector<TokenMatch>> e : groupedMatches)
		{
			std::sort(groupedMatches[e.first].begin(), groupedMatches[e.first].end(), [](const TokenMatch& a, const TokenMatch& b) {
				return a.precedence > b.precedence;
			});
			const TokenMatch* bestMatch = &groupedMatches[e.first].front();
			if (lastMatch != nullptr && bestMatch->start < lastMatch->end)
			{
				continue;
			}

			Token token;
			token.type = bestMatch->type;
			token.val = bestMatch->val;
			tokens.push_back(token);

			lastMatch = bestMatch;
		}
	}

	void findMatches(const std::string& input, const TokenDef& def, std::vector<TokenMatch>& matches)
	{
		std::cregex_iterator empty;
		for (auto it = std::cregex_iterator(input.c_str(), input.c_str() + input.length(), def.regex); it != empty; it++)
		{
			TokenMatch match;
			match.start = it->position(0);
			match.end = it->position(0) + it->length(0);
			match.val = it->str(0);
			match.type = def.type;
			match.precedence = def.precedence;
			matches.push_back(match);
		}
	}

	void addDefinition(int type, std::string regex, int precedence)
	{
		TokenDef def;
		def.type = type;
		def.regex = std::regex(regex);
		def.precedence = precedence;
		definitions.push_back(def);
	}

	void removeDefinitions()
	{
		definitions.clear();
	}
}