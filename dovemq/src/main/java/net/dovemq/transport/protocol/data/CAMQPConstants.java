package net.dovemq.transport.protocol.data;

public class CAMQPConstants
{



    public static final String AMQP_ERROR_NOT_ALLOWED_STR = "not-allowed";
    public static final String AMQP_ERROR_NOT_ALLOWED = "amqp:not-allowed";

    public static final String AMQP_ERROR_RESOURCE_LIMIT_EXCEEDED_STR = "resource-limit-exceeded";
    public static final String AMQP_ERROR_RESOURCE_LIMIT_EXCEEDED = "amqp:resource-limit-exceeded";

    public static final String AMQP_ERROR_INVALID_FIELD_STR = "invalid-field";
    public static final String AMQP_ERROR_INVALID_FIELD = "amqp:invalid-field";

    public static final String AMQP_ERROR_RESOURCE_DELETED_STR = "resource-deleted";
    public static final String AMQP_ERROR_RESOURCE_DELETED = "amqp:resource-deleted";

    public static final String AMQP_ERROR_DECODE_ERROR_STR = "decode-error";
    public static final String AMQP_ERROR_DECODE_ERROR = "amqp:decode-error";

    public static final String AMQP_ERROR_FRAME_SIZE_TOO_SMALL_STR = "frame-size-too-small";
    public static final String AMQP_ERROR_FRAME_SIZE_TOO_SMALL = "amqp:frame-size-too-small";

    public static final String AMQP_ERROR_RESOURCE_LOCKED_STR = "resource-locked";
    public static final String AMQP_ERROR_RESOURCE_LOCKED = "amqp:resource-locked";

    public static final String AMQP_ERROR_UNAUTHORIZED_ACCESS_STR = "unauthorized-access";
    public static final String AMQP_ERROR_UNAUTHORIZED_ACCESS = "amqp:unauthorized-access";

    public static final String AMQP_ERROR_NOT_IMPLEMENTED_STR = "not-implemented";
    public static final String AMQP_ERROR_NOT_IMPLEMENTED = "amqp:not-implemented";

    public static final String AMQP_ERROR_INTERNAL_ERROR_STR = "internal-error";
    public static final String AMQP_ERROR_INTERNAL_ERROR = "amqp:internal-error";

    public static final String AMQP_ERROR_ILLEGAL_STATE_STR = "illegal-state";
    public static final String AMQP_ERROR_ILLEGAL_STATE = "amqp:illegal-state";

    public static final String AMQP_ERROR_NOT_FOUND_STR = "not-found";
    public static final String AMQP_ERROR_NOT_FOUND = "amqp:not-found";

    public static final String AMQP_ERROR_PRECONDITION_FAILED_STR = "precondition-failed";
    public static final String AMQP_ERROR_PRECONDITION_FAILED = "amqp:precondition-failed";



    public static final String SESSION_ERROR_ERRANT_LINK_STR = "errant-link";
    public static final String SESSION_ERROR_ERRANT_LINK = "amqp:session:errant-link";

    public static final String SESSION_ERROR_WINDOW_VIOLATION_STR = "window-violation";
    public static final String SESSION_ERROR_WINDOW_VIOLATION = "amqp:session:window-violation";

    public static final String SESSION_ERROR_HANDLE_IN_USE_STR = "handle-in-use";
    public static final String SESSION_ERROR_HANDLE_IN_USE = "amqp:session:handle-in-use";

    public static final String SESSION_ERROR_UNATTACHED_HANDLE_STR = "unattached-handle";
    public static final String SESSION_ERROR_UNATTACHED_HANDLE = "amqp:session:unattached-handle";


    public static final String SENDER_SETTLE_MODE_UNSETTLED_STR = "unsettled";
    public static final int SENDER_SETTLE_MODE_UNSETTLED = 0;

    public static final String SENDER_SETTLE_MODE_SETTLED_STR = "settled";
    public static final int SENDER_SETTLE_MODE_SETTLED = 1;

    public static final String SENDER_SETTLE_MODE_MIXED_STR = "mixed";
    public static final int SENDER_SETTLE_MODE_MIXED = 2;




    public static final String STD_DIST_MODE_MOVE_STR = "move";
    public static final String STD_DIST_MODE_MOVE = "move";

    public static final String STD_DIST_MODE_COPY_STR = "copy";
    public static final String STD_DIST_MODE_COPY = "copy";



    public static final String TERMINUS_EXPIRY_POLICY_NEVER_STR = "never";
    public static final String TERMINUS_EXPIRY_POLICY_NEVER = "never";

    public static final String TERMINUS_EXPIRY_POLICY_CONNECTION_CLOSE_STR = "connection-close";
    public static final String TERMINUS_EXPIRY_POLICY_CONNECTION_CLOSE = "connection-close";

    public static final String TERMINUS_EXPIRY_POLICY_SESSION_END_STR = "session-end";
    public static final String TERMINUS_EXPIRY_POLICY_SESSION_END = "session-end";

    public static final String TERMINUS_EXPIRY_POLICY_LINK_DETACH_STR = "link-detach";
    public static final String TERMINUS_EXPIRY_POLICY_LINK_DETACH = "link-detach";





    public static final String LINK_ERROR_MESSAGE_SIZE_EXCEEDED_STR = "message-size-exceeded";
    public static final String LINK_ERROR_MESSAGE_SIZE_EXCEEDED = "amqp:link:message-size-exceeded";

    public static final String LINK_ERROR_DETACH_FORCED_STR = "detach-forced";
    public static final String LINK_ERROR_DETACH_FORCED = "amqp:link:detach-forced";

    public static final String LINK_ERROR_REDIRECT_STR = "redirect";
    public static final String LINK_ERROR_REDIRECT = "amqp:link:redirect";

    public static final String LINK_ERROR_STOLEN_STR = "stolen";
    public static final String LINK_ERROR_STOLEN = "amqp:link:stolen";

    public static final String LINK_ERROR_TRANSFER_LIMIT_EXCEEDED_STR = "transfer-limit-exceeded";
    public static final String LINK_ERROR_TRANSFER_LIMIT_EXCEEDED = "amqp:link:transfer-limit-exceeded";



    public static final String CONNECTION_ERROR_FRAMING_ERROR_STR = "framing-error";
    public static final String CONNECTION_ERROR_FRAMING_ERROR = "amqp:connection:framing-error";

    public static final String CONNECTION_ERROR_CONNECTION_FORCED_STR = "connection-forced";
    public static final String CONNECTION_ERROR_CONNECTION_FORCED = "amqp:connection:forced";

    public static final String CONNECTION_ERROR_REDIRECT_STR = "redirect";
    public static final String CONNECTION_ERROR_REDIRECT = "amqp:connection:redirect";


    public static final String SASL_CODE_SYS_TEMP_STR = "sys-temp";
    public static final int SASL_CODE_SYS_TEMP = 4;

    public static final String SASL_CODE_SYS_STR = "sys";
    public static final int SASL_CODE_SYS = 2;

    public static final String SASL_CODE_OK_STR = "ok";
    public static final int SASL_CODE_OK = 0;

    public static final String SASL_CODE_SYS_PERM_STR = "sys-perm";
    public static final int SASL_CODE_SYS_PERM = 3;

    public static final String SASL_CODE_AUTH_STR = "auth";
    public static final int SASL_CODE_AUTH = 1;







    public static final String TERMINUS_DURABILITY_NONE_STR = "none";
    public static final int TERMINUS_DURABILITY_NONE = 0;

    public static final String TERMINUS_DURABILITY_CONFIGURATION_STR = "configuration";
    public static final int TERMINUS_DURABILITY_CONFIGURATION = 1;

    public static final String TERMINUS_DURABILITY_UNSETTLED_STATE_STR = "unsettled-state";
    public static final int TERMINUS_DURABILITY_UNSETTLED_STATE = 2;





    public static final String RECEIVER_SETTLE_MODE_SECOND_STR = "second";
    public static final int RECEIVER_SETTLE_MODE_SECOND = 1;

    public static final String RECEIVER_SETTLE_MODE_FIRST_STR = "first";
    public static final int RECEIVER_SETTLE_MODE_FIRST = 0;









}
