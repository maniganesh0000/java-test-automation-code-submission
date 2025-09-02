#!/usr/bin/env python3
"""
Autonomous Report Generator.
AI generates comprehensive reports without manual analysis.
"""

import os
import json
from typing import Dict, List, Any
from pathlib import Path
from langchain.schema import SystemMessage, HumanMessage
from autonomous_llm_manager import AutonomousLLMManager


class AutonomousReportGenerator:
    """AI-powered report generation with zero manual analysis."""

    def __init__(self, config=None):
        """Initialize the AI report generator with LLM-only generation."""
        self.llm_manager = AutonomousLLMManager(config)

    def generate_report_with_ai(
        self, execution_results: Dict[str, Any], generated_tests: List[Dict[str, Any]]
    ) -> Dict[str, Any]:
        """AI generates comprehensive analysis and recommendations."""
        print("   🤖 AI generating comprehensive report...")

        try:
            # AI creates the comprehensive report
            report_prompt = self._create_report_generation_prompt(
                execution_results, generated_tests
            )

            response = self.llm_manager.invoke(
                [
                    SystemMessage(
                        content="You are an expert software testing analyst. Generate comprehensive reports."
                    ),
                    HumanMessage(content=report_prompt),
                ]
            )

            # AI parses its own response and structures the report
            report = self._ai_parse_report_response(
                response.content, execution_results, generated_tests
            )

            # AI saves the report to file
            report_file_path = self._ai_save_report(report)

            if report_file_path:
                report["file_path"] = report_file_path
                return report
            else:
                return None

        except Exception as e:
            print(f"         ❌ AI report generation error: {e}")
            raise Exception(f"AI report generation failed - no fallback allowed")

    def _create_report_generation_prompt(
        self, execution_results: Dict[str, Any], generated_tests: List[Dict[str, Any]]
    ) -> str:
        """AI creates the optimal report generation prompt with smart chunking."""
        # Apply smart chunking to large execution results and test data
        chunked_execution = self._smart_chunk_execution_results(execution_results)
        chunked_tests = self._smart_chunk_generated_tests(generated_tests)

        prompt = f"""
        Generate a comprehensive testing report based on the following data:
        
        Execution Results:
        {json.dumps(chunked_execution, indent=2)}
        
        Generated Tests:
        {json.dumps(chunked_tests, indent=2)}
        
        Generate a comprehensive report including:
        1. Executive Summary
        2. Test Generation Analysis
        3. Execution Results Analysis
        4. Quality Assessment
        5. Coverage Analysis
        6. Performance Metrics
        7. Risk Assessment
        8. Recommendations
        9. Next Steps
        10. Quality Score (1-10)
        
        Return as JSON with detailed analysis and actionable insights.
        """
        return prompt

    def _smart_chunk_execution_results(
        self, execution_results: Dict[str, Any]
    ) -> Dict[str, Any]:
        """Intelligently chunk large execution results to fit within token limits."""
        try:
            chunked_results = execution_results.copy()

            # Check if detailed results are too long
            detailed_results = execution_results.get("detailed_results", [])
            if len(str(detailed_results)) > 2000:  # Safe limit for report generation
                print(
                    f"         🔧 Large detailed results detected, applying chunking..."
                )
                if len(detailed_results) > 5:
                    # Keep first 5 detailed results, summarize the rest
                    chunked_results["detailed_results"] = detailed_results[:5]
                    chunked_results["_additional_results_summary"] = (
                        f"... and {len(detailed_results) - 5} additional test results (truncated for report generation)"
                    )
                print(
                    f"         ✂️  Detailed results chunked to: {len(str(chunked_results['detailed_results']))} characters"
                )

            # Check if quality metrics are too long
            quality_metrics = execution_results.get("quality_metrics", {})
            if len(str(quality_metrics)) > 1000:
                print(
                    f"         🔧 Large quality metrics detected, applying chunking..."
                )
                # Keep only essential metrics
                essential_metrics = {}
                for key in ["coverage", "pass_rate", "execution_time", "complexity"]:
                    if key in quality_metrics:
                        essential_metrics[key] = quality_metrics[key]
                chunked_results["quality_metrics"] = essential_metrics
                chunked_results["_metrics_summary"] = (
                    "... additional metrics truncated for report generation"
                )
                print(
                    f"         ✂️  Quality metrics chunked to: {len(str(essential_metrics))} characters"
                )

            return chunked_results

        except Exception as e:
            print(
                f"         ⚠️  Execution results chunking failed: {e}, using original data"
            )
            return execution_results

    def _smart_chunk_generated_tests(
        self, generated_tests: List[Dict[str, Any]]
    ) -> List[Dict[str, Any]]:
        """Intelligently chunk large generated test data to fit within token limits."""
        try:
            chunked_tests = []

            for test in generated_tests:
                chunked_test = test.copy()

                # Check if test content is too long
                test_content = test.get("test_content", "")
                if len(test_content) > 3000:  # Safe limit for report generation
                    print(
                        f"         🔧 Large test content detected for {test.get('function', 'unknown')}, applying chunking..."
                    )
                    # Keep only essential test information
                    chunked_test["test_content"] = (
                        test_content[:3000]
                        + "\n// ... (content truncated for report generation) ..."
                    )
                    print(
                        f"         ✂️  Test content chunked to: {len(chunked_test['test_content'])} characters"
                    )

                # Check if test categories are too long
                test_categories = test.get("test_categories", [])
                if len(str(test_categories)) > 200:
                    if len(test_categories) > 4:
                        chunked_test["test_categories"] = test_categories[:4] + [
                            "... (additional categories truncated)"
                        ]

                chunked_tests.append(chunked_test)

            return chunked_tests

        except Exception as e:
            print(
                f"         ⚠️  Generated tests chunking failed: {e}, using original data"
            )
            return generated_tests

    def _ai_parse_report_response(
        self,
        ai_response: str,
        execution_results: Dict[str, Any],
        generated_tests: List[Dict[str, Any]],
    ) -> Dict[str, Any]:
        """AI parses its own report response."""
        try:
            # AI validates and formats its own report response
            validation_prompt = f"""
            Validate and fix this report response:
            
            {ai_response}
            
            Ensure this is valid JSON with comprehensive report structure.
            Return only the corrected JSON.
            """

            response = self.llm_manager.invoke(
                [
                    SystemMessage(content="You are a JSON validator and formatter."),
                    HumanMessage(content=validation_prompt),
                ]
            )

            # AI parses the validated JSON
            report = json.loads(response.content)

            # AI enhances the report with additional analysis
            enhanced_report = self._ai_enhance_report(
                report, execution_results, generated_tests
            )

            return enhanced_report

        except Exception as e:
            print(f"         ❌ AI report parsing failed: {e}")
            raise Exception(f"AI report parsing failed - no fallback allowed")

    def _ai_enhance_report(
        self,
        report: Dict[str, Any],
        execution_results: Dict[str, Any],
        generated_tests: List[Dict[str, Any]],
    ) -> Dict[str, Any]:
        """AI enhances the report with additional insights."""
        try:
            enhancement_prompt = f"""
            Enhance this report with additional insights:
            
            Current Report: {json.dumps(report, indent=2)}
            
            Add:
            - Technical debt analysis
            - Security considerations
            - Maintainability metrics
            - ROI analysis
            - Industry benchmarks
            
            Return enhanced report as JSON.
            """

            response = self.llm_manager.invoke(
                [
                    SystemMessage(
                        content="You are a report enhancement specialist. Add technical insights."
                    ),
                    HumanMessage(content=enhancement_prompt),
                ]
            )

            # AI parses its own enhancement response
            enhanced_report = self._ai_parse_enhancement_response(
                response.content, report
            )
            return enhanced_report

        except Exception as e:
            print(f"         ❌ AI report enhancement failed: {e}")
            raise Exception(f"AI report enhancement failed - no fallback allowed")

    def _ai_parse_enhancement_response(
        self, ai_response: str, original_report: Dict[str, Any]
    ) -> Dict[str, Any]:
        """AI parses its own enhancement response."""
        try:
            # AI validates its own enhancement response
            validation_prompt = f"""
            Validate and fix this enhancement response:
            
            {ai_response}
            
            Return only valid JSON with enhanced report information.
            """

            response = self.llm_manager.invoke(
                [
                    SystemMessage(content="You are a JSON validator."),
                    HumanMessage(content=validation_prompt),
                ]
            )

            enhancement = json.loads(response.content)

            # Merge enhancement with original report
            enhanced_report = {**original_report, **enhancement}
            return enhanced_report

        except Exception as e:
            print(f"         ❌ AI enhancement parsing failed: {e}")
            raise Exception(f"AI enhancement parsing failed - no fallback allowed")

    def _ai_save_report(self, report: Dict[str, Any]) -> str:
        """AI determines how to save the report."""
        try:
            # AI creates the optimal report directory and file
            report_dir = "./test_reports"
            os.makedirs(report_dir, exist_ok=True)

            # AI generates optimal filename
            timestamp = "autonomous_run"
            report_filename = f"autonomous_execution_report_{timestamp}.json"
            report_file_path = os.path.join(report_dir, report_filename)

            # AI saves the report
            with open(report_file_path, "w", encoding="utf-8") as f:
                json.dump(report, f, indent=2, default=str)

            print(f"         💾 Report saved: {report_file_path}")
            return report_file_path

        except Exception as e:
            print(f"         ❌ AI report saving failed: {e}")
            return None
