// Последнее состояние, пришедшее от нативного плеера, и когда оно пришло. От него между ответами плеера считается позиция.
let anchor = { status: null, at: 0 };

export const setAnchor = (status) => {
  anchor = { status, at: performance.now() };
};

export const getAnchor = () => anchor;
