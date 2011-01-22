@lombok.ListenerSupport({java.awt.event.KeyListener.class, java.awt.event.MouseWheelListener.class})
class ListenerSupportPlain1 {
}
@lombok.ListenerSupport(java.lang.String.class)
class ListenerSupportPlain2 {
}
@lombok.ListenerSupport(javax.swing.event.MouseInputListener.class)
class ListenerSupportPlain3 {
}