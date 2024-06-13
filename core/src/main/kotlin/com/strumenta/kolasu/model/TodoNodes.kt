/**
 * Marker interface for a node in and AST that's not yet well-defined and is left as a "to-do" placeholder.
 *
 * We chose not to call this "Placeholder" because a placeholder is a more general concept; it could also be an
 * insertion point in a template, for example, while a TodoNode is a specific type of placeholder.
 */
interface TodoNode {
    val message: String
}
