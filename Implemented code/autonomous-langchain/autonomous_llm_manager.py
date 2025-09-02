#!/usr/bin/env python3
"""
Autonomous LLM Manager.
Centralized LLM management with Gemini API fallback to OpenAI.
"""

import os
from typing import Optional, Dict, Any, List
from langchain_openai import ChatOpenAI
from langchain.schema import SystemMessage, HumanMessage
from dotenv import load_dotenv

# Load .env file from parent directory
load_dotenv(os.path.join(os.path.dirname(__file__), "..", ".env"))


class AutonomousLLMManager:
    """Centralized LLM management with automatic fallback from Gemini to OpenAI."""

    def __init__(self, config=None):
        """Initialize the LLM manager with fallback support."""
        self.config = config
        self.gemini_api_key = os.getenv("GEMINI_API_KEY", "")
        self.openai_api_key = os.getenv("OPENAI_API_KEY", "")
        self.current_provider = None
        self.llm = None

        # Initialize with preferred provider (Gemini first, then OpenAI)
        self._initialize_llm()

    def _initialize_llm(self):
        """Initialize LLM with fallback mechanism."""
        # Try Gemini first if API key is available
        if self.gemini_api_key:
            try:
                self._try_gemini()
                if self.llm is not None:
                    self.current_provider = "gemini"
                    print("   🔮 Using Gemini API as primary LLM provider")
                    return
            except Exception as e:
                print(f"   ⚠️  Gemini API initialization failed: {e}")
                print("   🔄 Falling back to OpenAI API...")

        # Fallback to OpenAI
        if self.openai_api_key:
            try:
                self._try_openai()
                if self.llm is not None:
                    self.current_provider = "openai"
                    print("   🤖 Using OpenAI API as fallback LLM provider")
                    return
            except Exception as e:
                print(f"   ❌ OpenAI API initialization failed: {e}")

        # If both fail, raise an error
        raise Exception("Both Gemini and OpenAI API keys are missing or invalid")

    def _try_gemini(self):
        """Try to initialize Gemini API."""
        try:
            # Import Google's Generative AI
            import google.generativeai as genai

            # Configure Gemini
            genai.configure(api_key=self.gemini_api_key)

            # Create a custom LLM wrapper for Gemini
            self.llm = GeminiLLMWrapper(genai)

        except ImportError:
            print(
                "   ⚠️  google-generativeai package not installed. Install with: pip install google-generativeai"
            )
            raise Exception("Gemini package not available")
        except Exception as e:
            print(f"   ⚠️  Gemini API configuration failed: {e}")
            raise

    def _try_openai(self):
        """Try to initialize OpenAI API."""
        try:
            model_name = "gpt-4"
            temperature = 0.1
            max_tokens = 4000

            if self.config:
                model_name = getattr(self.config, "model_name", model_name)
                temperature = getattr(self.config, "temperature", temperature)
                max_tokens = getattr(self.config, "max_tokens", max_tokens)

            self.llm = ChatOpenAI(
                model_name=model_name,
                temperature=temperature,
                max_tokens=max_tokens,
                openai_api_key=self.openai_api_key,
                request_timeout=30,
            )

        except Exception as e:
            print(f"   ⚠️  OpenAI API configuration failed: {e}")
            raise

    def invoke(self, messages: List, **kwargs) -> Any:
        """Invoke the LLM with automatic fallback if needed."""
        try:
            response = self.llm.invoke(messages, **kwargs)

            # Clean response from both providers
            if hasattr(response, "content"):
                response.content = self._clean_llm_response(response.content)

            return response
        except Exception as e:
            print(f"   ⚠️  {self.current_provider.upper()} API call failed: {e}")

            # Try to fallback to the other provider
            if self.current_provider == "gemini" and self.openai_api_key:
                print("   🔄 Falling back to OpenAI API...")
                try:
                    self._try_openai()
                    self.current_provider = "openai"
                    print("   ✅ Successfully switched to OpenAI API")
                    response = self.llm.invoke(messages, **kwargs)

                    # Clean OpenAI response too
                    if hasattr(response, "content"):
                        response.content = self._clean_llm_response(response.content)

                    return response
                except Exception as fallback_error:
                    print(f"   ❌ OpenAI fallback also failed: {fallback_error}")
                    raise Exception(
                        f"Both {self.current_provider} and fallback API failed"
                    )

            elif self.current_provider == "openai" and self.gemini_api_key:
                print("   🔄 Falling back to Gemini API...")
                try:
                    self._try_gemini()
                    self.current_provider = "gemini"
                    print("   ✅ Successfully switched to Gemini API")
                    response = self.llm.invoke(messages, **kwargs)

                    # Clean Gemini response too
                    if hasattr(response, "content"):
                        response.content = self._clean_llm_response(response.content)

                    return response
                except Exception as fallback_error:
                    print(f"   ❌ Gemini fallback also failed: {fallback_error}")
                    raise Exception(
                        f"Both {self.current_provider} and fallback API failed"
                    )

            else:
                raise Exception(f"No fallback available for {self.current_provider}")

    def get_current_provider(self) -> str:
        """Get the current LLM provider."""
        return self.current_provider

    def is_gemini_available(self) -> bool:
        """Check if Gemini API is available."""
        return bool(self.gemini_api_key)

    def is_openai_available(self) -> bool:
        """Check if OpenAI API is available."""
        return bool(self.openai_api_key)

    def _clean_llm_response(self, response_text: str) -> str:
        """Clean LLM response from both Gemini and OpenAI to extract valid content."""
        # Remove leading/trailing whitespace
        response_text = response_text.strip()

        # Check if this looks like JSON (contains JSON structure)
        if (
            '"name"' in response_text
            or '"signature"' in response_text
            or '"summary"' in response_text
            or '"total_tests"' in response_text
        ):
            # This is JSON - extract the JSON part
            # Find the first { and last } to extract only the JSON part
            first_brace = response_text.find("{")
            last_brace = response_text.rfind("}")

            if first_brace != -1 and last_brace != -1 and last_brace > first_brace:
                response_text = response_text[first_brace : last_brace + 1]

            # Remove JSON comments (// comments)
            lines = response_text.split("\n")
            cleaned_lines = []
            for line in lines:
                # Remove // comments but preserve URLs
                if "//" in line and not ("http" in line or "www" in line):
                    comment_pos = line.find("//")
                    line = line[:comment_pos].rstrip()
                cleaned_lines.append(line)

            response_text = "\n".join(cleaned_lines)

            # Remove trailing commas before closing braces/brackets
            import re

            response_text = re.sub(r",(\s*[}\]])", r"\1", response_text)

        # Check if this looks like Java code (contains package declaration)
        elif "package " in response_text and (
            "public class" in response_text or "public interface" in response_text
        ):
            # This is Java code - extract the Java part

            # First, look for markdown code blocks
            java_start = -1
            if "```java" in response_text:
                java_start = response_text.find("```java") + 7
            elif "```" in response_text:
                # Find any code block
                code_block_start = response_text.find("```")
                # Look for package after the code block marker
                temp_text = response_text[code_block_start + 3 :]
                if "package " in temp_text:
                    java_start = code_block_start + 3

            # If no markdown, find the first package declaration
            if java_start == -1:
                java_start = response_text.find("package ")

            if java_start != -1:
                response_text = response_text[java_start:]

                # Clean up any remaining markdown at the start
                if response_text.startswith("```java"):
                    response_text = response_text[7:]
                elif response_text.startswith("```"):
                    response_text = response_text[3:]

                # Find the actual package declaration if we're still before it
                if not response_text.strip().startswith("package "):
                    package_pos = response_text.find("package ")
                    if package_pos != -1:
                        response_text = response_text[package_pos:]

            # Remove trailing markdown and explanatory text
            if response_text.endswith("```"):
                response_text = response_text[:-3]

            # Remove any trailing text after the last closing brace
            last_brace = response_text.rfind("}")
            if last_brace != -1:
                response_text = response_text[: last_brace + 1]

        # For other content, just remove markdown code blocks
        else:
            if response_text.startswith("```java"):
                response_text = response_text[7:]  # Remove ```java
            elif response_text.startswith("```json"):
                response_text = response_text[7:]  # Remove ```json
            elif response_text.startswith("```"):
                response_text = response_text[3:]  # Remove ```
            if response_text.endswith("```"):
                response_text = response_text[:-3]  # Remove trailing ```

        # Remove any extra content after the main content
        response_text = response_text.strip()

        return response_text


class GeminiLLMWrapper:
    """Wrapper to make Gemini API compatible with LangChain interface."""

    def __init__(self, genai):
        """Initialize the Gemini wrapper."""
        self.genai = genai
        self.model = genai.GenerativeModel("gemini-1.5-flash")

    def invoke(self, messages: List, **kwargs) -> Any:
        """Invoke Gemini API with LangChain-compatible interface."""
        try:
            # Convert LangChain messages to Gemini format
            prompt = self._convert_messages_to_prompt(messages)

            # Generate response
            response = self.model.generate_content(prompt)

            # Clean the response to extract valid JSON
            cleaned_response = self._clean_llm_response(response.text)

            # Return in LangChain format
            return GeminiResponse(cleaned_response)

        except Exception as e:
            raise Exception(f"Gemini API call failed: {e}")

    def _convert_messages_to_prompt(self, messages: List) -> str:
        """Convert LangChain messages to Gemini prompt format."""
        prompt_parts = []

        for message in messages:
            if isinstance(message, SystemMessage):
                prompt_parts.append(f"System: {message.content}")
            elif isinstance(message, HumanMessage):
                prompt_parts.append(f"Human: {message.content}")
            else:
                prompt_parts.append(str(message.content))

        return "\n\n".join(prompt_parts)

    def _clean_llm_response(self, response_text: str) -> str:
        """Clean LLM response from both Gemini and OpenAI to extract valid content."""
        # Remove leading/trailing whitespace
        response_text = response_text.strip()

        # Check if this looks like JSON (contains JSON structure)
        if (
            '"name"' in response_text
            or '"signature"' in response_text
            or '"summary"' in response_text
            or '"total_tests"' in response_text
        ):
            # This is JSON - extract the JSON part
            # Find the first { and last } to extract only the JSON part
            first_brace = response_text.find("{")
            last_brace = response_text.rfind("}")

            if first_brace != -1 and last_brace != -1 and last_brace > first_brace:
                response_text = response_text[first_brace : last_brace + 1]

            # Remove JSON comments (// comments)
            lines = response_text.split("\n")
            cleaned_lines = []
            for line in lines:
                # Remove // comments but preserve URLs
                if "//" in line and not ("http" in line or "www" in line):
                    comment_pos = line.find("//")
                    line = line[:comment_pos].rstrip()
                cleaned_lines.append(line)

            response_text = "\n".join(cleaned_lines)

            # Remove trailing commas before closing braces/brackets
            import re

            response_text = re.sub(r",(\s*[}\]])", r"\1", response_text)

        # Check if this looks like Java code (contains package declaration)
        elif "package " in response_text and (
            "public class" in response_text or "public interface" in response_text
        ):
            # This is Java code - extract the Java part

            # First, look for markdown code blocks
            java_start = -1
            if "```java" in response_text:
                java_start = response_text.find("```java") + 7
            elif "```" in response_text:
                # Find any code block
                code_block_start = response_text.find("```")
                # Look for package after the code block marker
                temp_text = response_text[code_block_start + 3 :]
                if "package " in temp_text:
                    java_start = code_block_start + 3

            # If no markdown, find the first package declaration
            if java_start == -1:
                java_start = response_text.find("package ")

            if java_start != -1:
                response_text = response_text[java_start:]

                # Clean up any remaining markdown at the start
                if response_text.startswith("```java"):
                    response_text = response_text[7:]
                elif response_text.startswith("```"):
                    response_text = response_text[3:]

                # Find the actual package declaration if we're still before it
                if not response_text.strip().startswith("package "):
                    package_pos = response_text.find("package ")
                    if package_pos != -1:
                        response_text = response_text[package_pos:]

            # Remove trailing markdown and explanatory text
            if response_text.endswith("```"):
                response_text = response_text[:-3]

            # Remove any trailing text after the last closing brace
            last_brace = response_text.rfind("}")
            if last_brace != -1:
                response_text = response_text[: last_brace + 1]

        # For other content, just remove markdown code blocks
        else:
            if response_text.startswith("```java"):
                response_text = response_text[7:]  # Remove ```java
            elif response_text.startswith("```json"):
                response_text = response_text[7:]  # Remove ```json
            elif response_text.startswith("```"):
                response_text = response_text[3:]  # Remove ```
            if response_text.endswith("```"):
                response_text = response_text[:-3]  # Remove trailing ```

        # Remove any extra content after the main content
        response_text = response_text.strip()

        return response_text


class GeminiResponse:
    """Wrapper to make Gemini response compatible with LangChain format."""

    def __init__(self, content: str):
        """Initialize the response wrapper."""
        self.content = content
