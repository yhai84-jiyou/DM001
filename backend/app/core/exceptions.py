class StateTransitionError(Exception):
    pass


class BusinessError(Exception):
    def __init__(self, message: str, code: int = 400):
        self.message = message
        self.code = code
