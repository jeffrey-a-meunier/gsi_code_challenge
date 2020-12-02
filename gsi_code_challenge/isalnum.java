

import java.util.LinkedList;

/**
 *
 * ██╗░██████╗░█████╗░██╗░░░░░███╗░░██╗██╗░░░██╗███╗░░░███╗
 * ██║██╔════╝██╔══██╗██║░░░░░████╗░██║██║░░░██║████╗░████║
 * ██║╚█████╗░███████║██║░░░░░██╔██╗██║██║░░░██║██╔████╔██║
 * ██║░╚═══██╗██╔══██║██║░░░░░██║╚████║██║░░░██║██║╚██╔╝██║
 * ██║██████╔╝██║░░██║███████╗██║░╚███║╚██████╔╝██║░╚═╝░██║
 * ╚═╝╚═════╝░╚═╝░░╚═╝╚══════╝╚═╝░░╚══╝░╚═════╝░╚═╝░░░░░╚═╝
 *
 * By Jeff Meunier
 * 
 * Over-the-top solution to the isAlNum problem.
 * http://www.cplusplus.com/reference/cctype/isalnum/
 * <p>
 * This uses 256 worker threads, where each thread is pre-defined to
 * respond with a true or false value to a single character in the
 * range 0 - 255. Checks are made to ensure communication messages
 * are of the correct type and have the correct message values.
 * <p>
 * The set of threads acts as a concurrent finite map of character-to-boolean.
 * <p>
 * The actual isAlNum method starts on line 265.
 * <p>
 *
 * To use:
 *   $ javac IsAlNum.java
 *   $ java IsAlNum
 */
public class IsAlNum {

  // Instance variables =============================================

  private WorkerThread[] _workers = new WorkerThread[256];
  private MsgQ _replyQ = new MsgQ();

  // Static inner classes ===========================================

  /**
   * Threads communicate by exchanging Message instances.
   */
  static class Message {

    protected Object _payload;
    private MsgQ _senderQ;

    public Message(MsgQ senderQ, Object payload) {
      _senderQ = senderQ;
      _payload = payload;
    }

    public Object getPayload() {
      return _payload;
    }

    public MsgQ getSenderQ() {
      return _senderQ;
    }
  }

  /**
   * An ErrorMessage has neither a payload nor a sender.
   */
  static class ErrorMessage extends Message {

    public ErrorMessage(String message) {
      super(null, message);
    }

    @Override
    public Object getPayload() {
      throw new RuntimeException((String)_payload);
    }

    @Override
    public MsgQ getSenderQ() {
      throw new RuntimeException((String)_payload);
    }
  }

  /**
   * Inter-thread communication happens through synchronous message queues.
   */
  static class MsgQ {

    private LinkedList<Message> _messages = new LinkedList<>();

    public synchronized Message deq() {
      while(_messages.isEmpty())
        try {
          this.wait();
        }
        catch(InterruptedException exn) {
          return null;
        }
      return _messages.pop();
    }

    public synchronized void enq(Message message) {
      _messages.add(message);
      this.notify();
    }
  }

  /**
   * A WorkerThread is bound to a specific character in the range 0 - 255.
   * At creation, it calculates and caches the boolean isAlNum value for
   * that character.
   * <p>
   * The thread awaits a message containing a character payload. If that character
   * matches the WorkerThread's bound character, the cached boolean value is
   * returned as a message to the sender, otherwise an ErrorMessage is returned
   * to the sender.
   */ 
  static class WorkerThread implements Runnable {

    private char _c;
    private boolean _isAlNum;
    private MsgQ _msgq = new MsgQ();
    private Thread _thread;

    WorkerThread(char c) {
      _c = c;
      _isAlNum = _isAlNum(c);  // nb: member var & method have same name
      _thread = new Thread(this);
      _thread.start();
    }

    @Override
    public void run() {
      while(true) {
        Message msg = _msgq.deq();
        if(msg == null)
          break;
        Object payload = msg.getPayload();
        Message reply = null;
        if(payload instanceof Character) {
          if((Character)payload == _c)
            reply = new Message(_msgq, _isAlNum);
          else
            reply = new ErrorMessage("WorkerThread expected " + _c + " but received " + payload);
        }
        else {
          reply = new ErrorMessage("WorkerThread expected a Character payload but received a " + payload.getClass().getSimpleName());
        }
        msg.getSenderQ().enq(reply);
      }
    }

    /**
     * Helper method to send a message to a message queue.
     */
    public void sendMessage(MsgQ replyQ, Object message) {
      Message msg = new Message(replyQ, message);
      _msgq.enq(msg);
    }

    public void terminate() {
      _msgq.enq(null);
    }

    private boolean _isAlNum(char c) {
      return c >= 'A' && c <= 'Z'
          || c >= 'a' && c <= 'z'
          || c >= '0' && c <= '9';
    }
  }

  // Static methods =================================================

  static boolean isAlNum_expected(char c) {
    return c >= 'A' && c <= 'Z'
        || c >= 'a' && c <= 'z'
        || c >= '0' && c <= '9';
  }
  
  /**
   * The main method creates a new instance of the IsAlNum class and then
   * runs a few ad-hoc tests on it.
   */
  public static void main(String[] args) {
    IsAlNum app = new IsAlNum();

    // Test all characters in the range 0 to 255
    for(int n=0; n<256; n++) {
      char c = (char)n;
      System.out.printf("trying %3d ", n);
      if(n > 31 && n < 127)
        System.out.print("'" + c + "' ");
      else
        System.out.print("    ");
      boolean actual = app.isAlNum(c);
      boolean expected = isAlNum_expected(c);
      System.out.printf("%5s : ", actual);
      System.out.println(actual == expected ? "success" : "failure");
      assert(actual == expected);
    }
    System.out.println("finished testing characters in the range 0 to 255");

    // Test some pathological characters
    try {
      app.isAlNum((char)256);
      assert(false); // should not get here
    }
    catch(RuntimeException exn) {}
    try {
      app.isAlNum((char)-1);
      assert(false); // should not get here
    }
    catch(RuntimeException exn) {}

    app.terminate();

    // Test worker thread internals

    class WorkerThread_TestClass1 extends WorkerThread {
      WorkerThread_TestClass1() {
        super((char)123);
      }
      public void test() {
        MsgQ replyQ = new MsgQ();
        this.sendMessage(replyQ, "foo");  // worker is expecting character
        Message reply = replyQ.deq();
        assert(ErrorMessage.class == reply.getClass());
        System.out.println("finished testing WorkerThraead internal non-character message");
      }
    }
    WorkerThread_TestClass1 wtt1 = new WorkerThread_TestClass1();
    wtt1.test();
    wtt1.terminate();

    class WorkerThread_TestClass2 extends WorkerThread {
      WorkerThread_TestClass2() {
        super((char)123);
      }
      public void test() {
        MsgQ replyQ = new MsgQ();
        this.sendMessage(replyQ, 0);  // worker is expecting 123
        Message reply = replyQ.deq();
        assert(ErrorMessage.class == reply.getClass());
        System.out.println("finished testing WorkerThraead internal out-of-range character message");
      }
    }
    WorkerThread_TestClass2 wtt2 = new WorkerThread_TestClass2();
    wtt2.test();
    wtt2.terminate();
  }

  // Instance methods ===============================================

  /**
   * Determines if the argument in <pre>c</pre> is in any of these ranges:
   * <ul>
   * <li>A - Z</li>
   * <li>a - z</li>
   * <li>0 - 9</li>
   * </ul>
   * 
   * @param c the character to test
   * @return true if the character is in the required range, else false
   */
  boolean isAlNum(char c) {
    int ic = (int)c;
    if(ic < 0 || ic > 255)
      throw new RuntimeException("Character '" + c + "' (" + ic + ") is outside the range 0 - 255");

    // get the worker thread for c
    WorkerThread worker = _workers[ic];

    // spawn new thread if it's not already running
    if(worker == null) {
      worker = _workers[ic] = new WorkerThread(c);
    }

    // send c to the thread, wait for reply
    worker.sendMessage(_replyQ, c);
    Message reply = _replyQ.deq();
    Object replyPayload = reply.getPayload();

    // check the reply type
    if(replyPayload instanceof Boolean)
      return (Boolean)replyPayload;
    throw new RuntimeException("WorkerThread reply contains unexpected payload type: " + replyPayload.getClass().getSimpleName());
  }

  void terminate() {
    for(WorkerThread worker : _workers) {
      worker.terminate();
    }
  }

}
