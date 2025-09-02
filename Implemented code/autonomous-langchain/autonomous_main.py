#!/usr/bin/env python3
"""
Fully Autonomous LangChain Test Generation System.
Everything is handled by AI API calls - minimal code manipulation.
"""

import os
import sys
import json
from pathlib import Path
from typing import Dict, Any
from dotenv import load_dotenv

# Load .env file from parent directory
load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))

# Add current directory to path for imports
sys.path.append(os.path.dirname(os.path.abspath(__file__)))


def create_autonomous_config():
    """Create configuration using AI-generated settings."""
    try:
        from autonomous_config import AutonomousConfig

        config = AutonomousConfig()
        config.validate()
        print("✅ Autonomous configuration loaded from AI-generated settings!")
        return config
    except Exception as e:
        print(f"⚠️  Configuration error: {e}")
        return None


def autonomous_source_analysis():
    """Use AI to analyze source code without manual parsing."""
    print("🔍 Phase 1: AI-Powered Source Code Analysis")
    print("-" * 50)

    try:
        from autonomous_source_analyzer import AutonomousSourceAnalyzer

        config = create_autonomous_config()
        analyzer = AutonomousSourceAnalyzer(config)

        # AI analyzes the source code and returns function information
        analyzed_functions = analyzer.analyze_with_ai()

        print(f"📊 AI Analysis Complete:")
        print(f"   Total functions analyzed: {len(analyzed_functions)}")

        for func in analyzed_functions:
            print(
                f"   • {func['name']}: {func['complexity']} complexity, {func.get('lines_of_code', 'N/A')} lines"
            )

        return analyzed_functions

    except Exception as e:
        print(f"   ❌ AI analysis failed: {e}")
        print("   🚫 No fallback data allowed - system requires AI-generated analysis")
        return []


def autonomous_strategy_selection(analyzed_functions):
    """Use AI to select optimal testing strategies."""
    print("\n🧠 Phase 2: AI-Powered Strategy Selection")
    print("-" * 50)

    try:
        from autonomous_strategy_selector import AutonomousStrategySelector

        config = create_autonomous_config()
        selector = AutonomousStrategySelector(config)

        # AI selects the best strategies for each function
        selected_strategies = selector.select_strategies_with_ai(analyzed_functions)

        print(f"🎯 AI Strategy Selection Complete:")
        for func, strategy in selected_strategies:
            print(f"   • {func['name']}: {strategy['name']} ({strategy['reason']})")

        return selected_strategies

    except Exception as e:
        print(f"   ❌ AI strategy selection failed: {e}")
        print(
            "   🚫 No fallback strategies allowed - system requires AI-generated strategies"
        )
        return []


def autonomous_test_generation(selected_strategies):
    """Use AI to generate comprehensive test suites."""
    print("\n🧪 Phase 3: AI-Powered Test Generation")
    print("-" * 50)

    try:
        from autonomous_test_generator import AutonomousTestGenerator

        config = create_autonomous_config()
        generator = AutonomousTestGenerator(config)

        # AI generates complete test suites
        generated_tests = generator.generate_tests_with_ai(selected_strategies)

        print(f"📝 AI Test Generation Complete:")
        print(f"   Total test files generated: {len(generated_tests)}")

        for test in generated_tests:
            print(
                f"   • {test['function']}: {test['test_count']} tests, {test['coverage']}% coverage"
            )

        return generated_tests

    except Exception as e:
        print(f"   ❌ AI test generation failed: {e}")
        print("   🚫 No fallback tests allowed - system requires AI-generated tests")
        return []


def autonomous_test_execution(generated_tests):
    """Use AI to execute and analyze tests."""
    print("\n🏃 Phase 4: AI-Powered Test Execution")
    print("-" * 50)

    try:
        from autonomous_test_executor import AutonomousTestExecutor

        config = create_autonomous_config()
        executor = AutonomousTestExecutor(config)

        # AI handles test execution and analysis
        execution_results = executor.execute_tests_with_ai(generated_tests)

        print(f"⚡ AI Test Execution Complete:")
        print(f"   Total tests executed: {execution_results['total_tests']}")
        print(f"   Tests passed: {execution_results['passed_tests']}")
        print(f"   Coverage achieved: {execution_results['coverage']}%")

        return execution_results

    except Exception as e:
        print(f"   ❌ AI test execution failed: {e}")
        print(
            "   🚫 No fallback execution allowed - system requires AI-generated execution"
        )
        return {}


def autonomous_report_generation(execution_results, generated_tests):
    """Use AI to generate comprehensive reports."""
    print("\n📊 Phase 5: AI-Powered Report Generation")
    print("-" * 50)

    try:
        from autonomous_report_generator import AutonomousReportGenerator

        config = create_autonomous_config()
        generator = AutonomousReportGenerator(config)

        # AI generates comprehensive analysis and recommendations
        final_report = generator.generate_report_with_ai(
            execution_results, generated_tests
        )

        print(f"📋 AI Report Generation Complete:")
        print(f"   Report saved to: {final_report['file_path']}")
        print(f"   Analysis quality: {final_report['quality_score']}/10")
        print(f"   Recommendations: {len(final_report['recommendations'])} items")

        return final_report

    except Exception as e:
        print(f"   ❌ AI report generation failed: {e}")
        print(
            "   🚫 No fallback reports allowed - system requires AI-generated reports"
        )
        return {}


def main():
    """Main autonomous function - everything handled by AI."""
    print("🤖 Fully Autonomous LangChain Test Generation System")
    print("=" * 60)
    print("🎯 Everything handled by AI API calls - minimal code manipulation")
    print("")

    config = create_autonomous_config()
    if not config:
        print("❌ Autonomous configuration failed. Cannot continue.")
        return 1

    # Phase 1: AI analyzes source code
    analyzed_functions = autonomous_source_analysis()
    if not analyzed_functions:
        print("❌ AI source analysis failed. Cannot continue.")
        return 1

    # Phase 2: AI selects strategies
    selected_strategies = autonomous_strategy_selection(analyzed_functions)
    if not selected_strategies:
        print("❌ AI strategy selection failed. Cannot continue.")
        return 1

    # Phase 3: AI generates tests
    generated_tests = autonomous_test_generation(selected_strategies)
    if not generated_tests:
        print("❌ AI test generation failed. Cannot continue.")
        return 1

    # Phase 4: AI executes tests
    execution_results = autonomous_test_execution(generated_tests)
    if not execution_results:
        print("❌ AI test execution failed. Cannot continue.")
        return 1

    # Phase 5: AI generates reports
    final_report = autonomous_report_generation(execution_results, generated_tests)
    if not final_report:
        print("❌ AI report generation failed.")
        return 1

    # Final summary
    print("\n🎉 Fully Autonomous Run Completed Successfully!")
    print("=" * 60)
    print("🤖 Every aspect handled by AI:")
    print("   ✅ Source code analysis")
    print("   ✅ Strategy selection")
    print("   ✅ Test generation")
    print("   ✅ Test execution")
    print("   ✅ Report generation")
    print("")
    print("📁 Generated files:")
    print(f"   📁 ./generated_tests/ - {len(generated_tests)} test files")
    print(f"   📊 ./test_reports/ - AI-generated execution report")
    print("")
    print("🚀 Next steps:")
    print("1. Review AI-generated test cases")
    print("2. AI will suggest improvements")
    print("3. Integrate with CI/CD pipeline")
    print("4. Let AI handle future test maintenance")

    return 0


if __name__ == "__main__":
    sys.exit(main())
