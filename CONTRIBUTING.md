# Code Contributing

**Thank you for your interest in contributing code to KeePassDX!** As a security-centric application under the GNU GPLv3, our priorities are code integrity, transparency, and human accountability.

*Pull requests (PRs) are highly encouraged, provided they adhere to the security and architectural standards outlined below.*

## Feature Contributions

If the issue has been discussed in a dedicated issue and the need has been established, you can [fork](https://help.github.com/en/github/getting-started-with-github/fork-a-repo) the application.

Make sure to follow the guidelines outlined in this document before submitting your [pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/proposing-changes-to-your-work-with-pull-requests) to effectively design and develop a feature.

## General Guidelines

KeePassDX was designed to be a purely local application with **no unnecessary permissions** and **no internet connection**. Please avoid adding features that would require an external network connection.

It should remain a **file-editing application** that uses the Storage Access Framework and does not directly manipulate database files.

The app should remain as **lightweight** as possible.

Keep the **code simple and manageable**. If it becomes too complex, delegate responsibility to appropriate managers or modules.

Pay special attention to **security and user experience**. If you have to choose between usability and security, prioritize security, but keep in mind a solution that minimize user frustration as much as possible.

## Code Quality

Ensure your code follows the **existing architectural patterns** and Android best practices used in the project.

**Never duplicate** a concept, a function, or a piece of code, and ensure that a feature is consistent with the application’s guidelines. Keep your code consistent with the style, formatting, and conventions in the rest of the code base.

KeePassDX is open to a paradigm shift or architectural change if the benefits outweigh the drawbacks, but this requires thorough analysis and discussion.

## Security

Every change must be **auditable**. Code that introduces obfuscation or non-deterministic behavior will be rejected.

All pull requests will undergo a manual review process — just having a feature work as expected is not enough for approval. Every line of code, including dependencies, should be auditable and testable through both practical and theoretical stress tests.

## Copyleft

All contributions must be compatible with the [GPL-3.0 License](https://www.gnu.org/licenses/gpl-3.0.html). By submitting a pull request, the author agrees to license their contribution in accordance with these terms.

## Code Generation

The author must explicitly state that they have manually reviewed, tested, and understood every line of the generated code and ensures that the generated code complies with all rules—whether ethical or regulatory — and does not cause harm to anyone. All code must clearly identify its author, whether it is artificial or not.

# Submission Requirements

Be specific about the expected outcome of the feature and how it integrates with existing features. If possible, include implementation details, as well as technical and architectural decisions made during development.

## Tests

Please ensure thorough testing of your changes. The more unit and UI tests you can provide, the better. Also, check for potential regressions. Provide as much detail as possible about the conditions under which tests were run (e.g., Android version, reproduction steps).

## Documentation

Document your changes with code comments or by updating existing documentation to reflect the new functionality.

## Commits

Keep your commits clean and avoid "monolithic" changes. Break down changes into logical steps and use descriptive commit messages. Make sure to keep your pull request up to date with the develop branch for smoother merging. Avoid including unrelated changes in your PR, such as modifications to CI/CD files.

## Dependencies

Avoid unnecessary dependencies: KeePassDX aims to be lightweight. Adding new libraries requires strong justification and approval from the maintainers. If you've incremented the version numbers of existing libraries, make sure to include them in a separate commit.

# Licensing

By contributing to KeePassDX, you confirm that you are the original author of the code or have the right to submit it. You agree to license your contribution under the GPL-3.0 license. As this is a maintainer-led project, your pull request may be modified or rejected to ensure that the app maintains its standards.