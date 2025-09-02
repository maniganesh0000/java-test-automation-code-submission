#!/usr/bin/env python3
"""
Autonomous Source Code Analyzer.
AI analyzes Java source code without manual parsing or manipulation.
"""

import os
import json
from typing import Dict, List, Any
from pathlib import Path
from langchain.schema import SystemMessage, HumanMessage
from autonomous_llm_manager import AutonomousLLMManager


class AutonomousSourceAnalyzer:
    """AI-powered source code analysis with zero manual parsing."""

    def __init__(self, config):
        """Initialize with AI configuration and fallback support."""
        self.config = config
        self.llm_manager = AutonomousLLMManager(config)

    def analyze_with_ai(self) -> List[Dict[str, Any]]:
        """AI analyzes source code and returns function information."""
        print("   🤖 AI analyzing source code...")

        analyzed_functions = []

        for func_config in self.config.target_functions:
            print(f"      📁 Analyzing: {func_config['name']}")

            # AI reads and analyzes the source file
            function_info = self._ai_analyze_function(func_config)

            if function_info:
                analyzed_functions.append(function_info)
                print(f"      ✅ AI analysis complete for {func_config['name']}")
            else:
                print(f"      ⚠️  AI analysis failed for {func_config['name']}")

        return analyzed_functions

    def _ai_analyze_function(self, func_config: Dict[str, Any]) -> Dict[str, Any]:
        """AI analyzes a single function using the source file."""
        try:
            # AI reads the source file content
            source_content = self._ai_read_source_file(func_config)

            if not source_content:
                return self._ai_generate_mock_function_info(func_config)

            # AI analyzes the function and extracts information
            analysis_prompt = self._create_analysis_prompt(func_config, source_content)

            response = self.llm_manager.invoke(
                [
                    SystemMessage(
                        content="You are an expert Java code analyzer. Extract detailed function information."
                    ),
                    HumanMessage(content=analysis_prompt),
                ]
            )

            # AI parses its own response and structures the data
            function_info = self._ai_parse_analysis_response(
                response.content, func_config
            )

            return function_info

        except Exception as e:
            print(f"         ❌ AI analysis error: {e}")
            raise Exception(
                f"AI analysis failed for {func_config['name']} - no fallback allowed"
            )

    def _ai_read_source_file(self, func_config: Dict[str, Any]) -> str:
        """AI reads the source file content with smart chunking for large files."""
        file_path = os.path.join(self.config.source_code_path, func_config["file"])

        if not os.path.exists(file_path):
            print(f"         ⚠️  Source file not found: {file_path}")
            return ""

        try:
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()

            print(f"         📖 Source file loaded: {len(content)} characters")

            # Smart chunking for large files to avoid token limits
            if len(content) > 15000:  # GPT-4 safe limit
                print(f"         🔧 Large file detected, applying smart chunking...")
                content = self._smart_chunk_content(content, func_config)
                print(f"         ✂️  Content chunked to: {len(content)} characters")

            return content

        except Exception as e:
            print(f"         ❌ Error reading source file: {e}")
            return ""

    def _ai_generate_mock_function_info(
        self, func_config: Dict[str, Any]
    ) -> Dict[str, Any]:
        """AI generates mock function information when source is unavailable."""
        mock_prompt = f"""
        Generate mock function information for: {func_config['name']}
        
        Package: {func_config['package']}
        File: {func_config['file']}
        
        Create realistic mock data including:
        - Method signature
        - Parameters
        - Return type
        - Complexity metrics
        - Dependencies
        - Business logic description
        
        Return as JSON format.
        """

        try:
            response = self.llm_manager.invoke(
                [
                    SystemMessage(
                        content="You are an expert Java developer. Generate realistic mock function data."
                    ),
                    HumanMessage(content=mock_prompt),
                ]
            )

            # AI parses its own response
            mock_info = self._ai_parse_mock_response(response.content, func_config)
            return mock_info

        except Exception as e:
            print(f"         ❌ AI mock generation failed: {e}")
            raise Exception(
                f"AI mock generation failed for {func_config['name']} - no fallback allowed"
            )

    def _create_analysis_prompt(
        self, func_config: Dict[str, Any], source_content: str
    ) -> str:
        """AI creates the optimal analysis prompt."""
        prompt = f"""
        {self.config.get_ai_prompt('source_analysis')}
        
        Target Function: {func_config['name']}
        Package: {func_config['package']}
        File: {func_config['file']}
        
        Source Code:
        {source_content}
        
        Extract and return the following information in JSON format:
        {{
            "name": "function name",
            "signature": "full method signature",
            "parameters": [
                {{"name": "param1", "type": "String", "description": "..."}}
            ],
            "return_type": "return type",
            "complexity": "estimated complexity score 1-10",
            "lines_of_code": "approximate line count",
            "dependencies": ["list of external classes"],
            "business_logic": "description of what the function does",
            "test_scenarios": ["list of test scenarios to cover"]
        }}
        
        Ensure the response is valid JSON.
        """
        return prompt

    def _ai_parse_analysis_response(
        self, ai_response: str, func_config: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Parse AI analysis response with fallback to mock data."""
        try:
            # Try to parse the response directly first
            function_info = json.loads(ai_response)
            function_info["source_file"] = func_config["file"]
            function_info["package"] = func_config["package"]
            # Add full source file path for test generation and execution
            function_info["source_file_path"] = os.path.join(
                self.config.source_code_path, func_config["file"]
            )
            return function_info

        except json.JSONDecodeError:
            print(f"         ❌ JSON parsing failed - AI response invalid")
            raise Exception(
                f"AI analysis parsing failed for {func_config['name']} - no fallback allowed"
            )
        except Exception as e:
            print(f"         ❌ Response parsing failed: {e}")
            raise Exception(
                f"AI analysis parsing failed for {func_config['name']} - no fallback allowed"
            )

    def _ai_parse_mock_response(
        self, ai_response: str, func_config: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Parse mock response with fallback to basic data."""
        try:
            # Try to parse the response directly first
            function_info = json.loads(ai_response)
            function_info["source_file"] = func_config["file"]
            function_info["package"] = func_config["package"]
            function_info["is_mock"] = True
            # Add full source file path for test generation and execution
            function_info["source_file_path"] = os.path.join(
                self.config.source_code_path, func_config["file"]
            )
            return function_info

        except json.JSONDecodeError:
            print(f"         ❌ Mock JSON parsing failed - AI response invalid")
            raise Exception(
                f"AI mock generation parsing failed for {func_config['name']} - no fallback allowed"
            )
        except Exception as e:
            print(f"         ❌ Mock response parsing failed: {e}")
            raise Exception(
                f"AI mock generation parsing failed for {func_config['name']} - no fallback allowed"
            )

    def _smart_chunk_content(self, content: str, func_config: Dict[str, Any]) -> str:
        """Intelligently chunk large source files to fit within token limits."""
        try:
            # Find the target function in the large file
            function_name = func_config["name"]
            lines = content.split("\n")

            # Look for the function definition
            function_start = -1
            function_end = -1

            for i, line in enumerate(lines):
                if function_name in line and (
                    "public" in line or "private" in line or "protected" in line
                ):
                    function_start = i
                    break

            if function_start != -1:
                # Find the function end by looking for matching braces
                brace_count = 0
                for i in range(function_start, len(lines)):
                    line = lines[i]
                    brace_count += line.count("{") - line.count("}")

                    if brace_count == 0 and i > function_start:
                        function_end = i + 1
                        break

                if function_end == -1:
                    function_end = min(
                        function_start + 100, len(lines)
                    )  # Fallback: next 100 lines

                # Extract function with some context
                start_context = max(0, function_start - 10)  # 10 lines before
                end_context = min(len(lines), function_end + 10)  # 10 lines after

                chunked_content = "\n".join(lines[start_context:end_context])

                # If still too large, truncate intelligently
                if len(chunked_content) > 15000:
                    # Keep function signature and first part, truncate middle
                    signature_lines = chunked_content.split("\n")[:20]  # First 20 lines
                    chunked_content = (
                        "\n".join(signature_lines)
                        + "\n// ... (content truncated for analysis) ...\n"
                    )

                return chunked_content

            # Fallback: return first 15000 characters with context
            return content[:15000] + "\n// ... (content truncated for analysis) ..."

        except Exception as e:
            print(f"         ⚠️  Smart chunking failed: {e}, using simple truncation")
            return content[:15000] + "\n// ... (content truncated for analysis) ..."
