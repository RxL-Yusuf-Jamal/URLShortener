import React from 'react'
import Modal from '../ui/Modal';
import CreateNewShorten from './CreateNewShorten';

const ShortenPopUp = ({ open, setOpen, refetch}) => {

    const handleClose = () => {
        setOpen(false);
    };

  return (
    <Modal open={open} onClose={handleClose}>
        <CreateNewShorten setOpen={setOpen} refetch={refetch} />
    </Modal>
  )
}

export default ShortenPopUp;
