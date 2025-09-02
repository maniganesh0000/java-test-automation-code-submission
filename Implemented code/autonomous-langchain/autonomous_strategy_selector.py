#!/usr/bin/env python3
"""
Autonomous Strategy Selector.
AI selects optimal testing strategies for each function.
"""

import json
from typing import Dict, List, Any, Tuple
from langchain.schema import SystemMessage, HumanMessage
from autonomous_llm_manager import AutonomousLLMManager


class AutonomousStrategySelector:
    """AI-powered strategy selection with zero manual logic."""

    def __init__(self, config=None):
        """Initialize the AI strategy selector with fallback support."""
        self.llm_manager = AutonomousLLMManager(config)

    def select_strategies_with_ai(
        self, analyzed_functions: List[Dict[str, Any]]
    ) -> List[Tuple[Dict[str, Any], Dict[str, Any]]]:
        """AI selects the best testing strategy for each function."""
        print("   🤖 AI selecting optimal testing strategies...")

        selected_strategies = []

        for function_info in analyzed_functions:
            print(f"      🎯 Selecting strategy for: {function_info['name']}")

            # AI analyzes the function and selects the best strategy
            strategy = self._ai_select_strategy(function_info)

            if strategy:
                selected_strategies.append((function_info, strategy))
                print(f"      ✅ Strategy selected: {strategy['name']}")
            else:
                print(f"      ⚠️  Strategy selection failed for {function_info['name']}")

        return selected_strategies

    def _ai_select_strategy(self, function_info: Dict[str, Any]) -> Dict[str, Any]:
        """AI selects the optimal testing strategy for a function."""
        try:
            # AI analyzes function characteristics and selects strategy
            selection_prompt = self._create_strategy_selection_prompt(function_info)

            response = self.llm_manager.invoke(
                [
                    SystemMessage(
                        content="You are an expert software testing strategist. You MUST return ONLY valid JSON responses. No markdown, no explanations, no additional text - just the JSON object."
                    ),
                    HumanMessage(content=selection_prompt),
                ]
            )

            # AI parses its own response and structures the strategy
            strategy = self._ai_parse_strategy_response(response.content, function_info)

            return strategy

        except Exception as e:
            print(f"         ❌ AI strategy selection error: {e}")
            raise Exception(
                f"AI strategy selection failed for {function_info['name']} - no fallback allowed"
            )

    def _create_strategy_selection_prompt(self, function_info: Dict[str, Any]) -> str:
        """AI creates the optimal strategy selection prompt with smart chunking."""
        # Apply smart chunking to large function information
        chunked_info = self._smart_chunk_function_info(function_info)

        prompt = f"""
        You are a testing strategy expert. Analyze this function and select the optimal testing strategy.

        FUNCTION TO ANALYZE:
        - Name: {chunked_info['name']}
        - Signature: {chunked_info.get('signature', 'N/A')}
        - Parameters: {len(chunked_info.get('parameters', []))}
        - Return Type: {chunked_info.get('return_type', 'N/A')}
        - Complexity: {chunked_info.get('complexity', 'N/A')}/10
        - Lines of Code: {chunked_info.get('lines_of_code', 'N/A')}
        - Dependencies: {chunked_info.get('dependencies', [])}
        - Business Logic: {chunked_info.get('business_logic', 'N/A')}
        
        AVAILABLE TESTING STRATEGIES:
        1. Comprehensive - Covers all code paths, edge cases, and scenarios
        2. Edge Case Focused - Tests boundary conditions and unusual inputs
        3. Boundary Value - Tests limits and threshold conditions
        4. Error Scenario - Tests exception handling and error paths
        5. Performance Oriented - Tests execution time and resource usage
        
        INSTRUCTIONS:
        - Analyze the function characteristics above
        - Select the BEST testing strategy from the available options
        - Return ONLY valid JSON with no additional text, markdown, or explanations
        
        REQUIRED JSON FORMAT (copy this exactly and fill in the values):
        {{
            "name": "strategy name",
            "description": "why this strategy is optimal",
            "priority": "high/medium/low",
            "test_categories": ["list of test categories to focus on"],
            "estimated_test_count": "estimated number of tests needed",
            "coverage_target": "target coverage percentage",
            "complexity_factors": ["factors that influenced selection"],
            "reason": "detailed explanation of selection"
        }}
        
        CRITICAL: Return ONLY the JSON object above. No markdown, no explanations, no additional text.
        """
        return prompt

    def _smart_chunk_function_info(
        self, function_info: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Intelligently chunk large function information to fit within token limits."""
        try:
            chunked_info = function_info.copy()

            # Check if business logic is too long
            business_logic = function_info.get("business_logic", "")
            if len(business_logic) > 500:  # Safe limit for strategy selection
                print(
                    f"         🔧 Large business logic detected, applying chunking..."
                )
                chunked_info["business_logic"] = (
                    business_logic[:500] + "... (truncated for strategy selection)"
                )
                print(
                    f"         ✂️  Business logic chunked to: {len(chunked_info['business_logic'])} characters"
                )

            # Check if dependencies list is too long
            dependencies = function_info.get("dependencies", [])
            if len(str(dependencies)) > 300:
                print(f"         🔧 Large dependencies detected, applying chunking...")
                if len(dependencies) > 10:
                    chunked_info["dependencies"] = dependencies[:10] + [
                        "... (additional dependencies truncated)"
                    ]
                print(
                    f"         ✂️  Dependencies chunked to: {len(str(chunked_info['dependencies']))} characters"
                )

            # Check if test scenarios are too long
            test_scenarios = function_info.get("test_scenarios", [])
            if len(str(test_scenarios)) > 400:
                print(
                    f"         🔧 Large test scenarios detected, applying chunking..."
                )
                if len(test_scenarios) > 8:
                    chunked_info["test_scenarios"] = test_scenarios[:8] + [
                        "... (additional scenarios truncated)"
                    ]
                print(
                    f"         ✂️  Test scenarios chunked to: {len(str(chunked_info['test_scenarios']))} characters"
                )

            return chunked_info

        except Exception as e:
            print(f"         ⚠️  Smart chunking failed: {e}, using original data")
            return function_info

    def _ai_parse_strategy_response(
        self, ai_response: str, function_info: Dict[str, Any]
    ) -> Dict[str, Any]:
        """AI parses its own strategy response."""
        try:
            # AI validates and parses its own JSON response
            validation_prompt = f"""
            You are a JSON validator. The following response should be valid JSON for a testing strategy.
            
            RESPONSE TO VALIDATE:
            {ai_response}
            
            INSTRUCTIONS:
            - If the response is valid JSON, return it exactly as is
            - If the response is NOT valid JSON, extract the JSON part or generate a corrected version
            - Return ONLY valid JSON - no explanations, no markdown, no additional text
            
            CRITICAL: Return ONLY the JSON object. No other text.
            """

            response = self.llm_manager.invoke(
                [
                    SystemMessage(
                        content="You are a JSON validator. You MUST return ONLY valid JSON. No explanations, no markdown, no additional text - just the JSON object."
                    ),
                    HumanMessage(content=validation_prompt),
                ]
            )

            # Parse the validated JSON
            strategy = json.loads(response.content)
            strategy["function_name"] = function_info["name"]
            strategy["selected_at"] = "AI-generated"

            return strategy

        except Exception as e:
            print(f"         ❌ AI strategy parsing failed: {e}")
            raise Exception(
                f"AI strategy parsing failed for {function_info['name']} - no fallback allowed"
            )
