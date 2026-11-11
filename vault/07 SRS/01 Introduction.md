---
type: srs
---

# 01 Introduction

Part of [[00 SRS index]].

## Purpose

This specification defines the final BlueprintQA product: a local, one-command browser-test studio where a QA engineer builds a test by connecting blueprint steps, runs it on a Selenium Grid, and reads a full step-by-step result. The same blueprint can run on a schedule or from CI.

This document is the contract for development. [[Development sequence]] says when each requirement is implemented. [[Requirement register]] says whether it is done.

## Product idea

The canvas is an Unreal-style blueprint, not a general automation builder. See [[Blueprint not n8n]] and [[Direction correction]].

- White execution pins order the steps.
- Colored data pins pass an element, text, or boolean.
- Python stores the blueprint, validates it, schedules it, and streams feedback.
- Java executes the blueprint on Selenium Grid.
- The studio never asks the user to write Selenium code for a normal test.

## Intended readers

- The person building the product, step by step.
- A later agent or contributor who must not invent a second product.
- A QA user checking whether the finished tool matches what was asked.

## References already in the repo

- [[System map]]
- [[Pins and steps]]
- [[Automatic runs]]
- [[Step feedback]]
- [[00 Board]]
