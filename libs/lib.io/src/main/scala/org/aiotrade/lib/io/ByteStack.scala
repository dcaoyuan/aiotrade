package org.aiotrade.lib.io

class ByteStack {
    
  private val STACK_INIT_SIZE = 16
  private var stack = new Array[Byte](STACK_INIT_SIZE)
  private var ptr = 0 // pointer into the stack of parser states

  /**
   * push current parser state (use at start of new container)
   */
  def push(b: Byte) {
    if (ptr >= stack.length) {
      val newStack = new Array[Byte](stack.length + STACK_INIT_SIZE)
      Array.copy(stack, 0, newStack, 0, stack.length)
      stack = newStack
    }
    stack(ptr) = b
    ptr += 1
  }

  /**
   * pop  parser state (use at end of container)
   */
  def pop: Byte = {
    ptr -= 1
    peak
  }

  def peak = if (ptr < 0) throw new RuntimeException("Unbalanced container") else stack(ptr)
    
  /**
   * @return the current nesting level, the number of parent objects or arrays.
   */
  def getLevel = ptr
}
